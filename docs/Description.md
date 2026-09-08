# Functional Connections!

## 1. Quick Start

### Linux

**Scripts**
- `./scripts/run.sh` to start the server in the background and see the client's output
- `./scripts/build.sh` to compile the client and the server into separate files in `/out`
- Both load separate config files from `/config`

**By hand**

```bash
javac -cp "lib/gson-2.10.1.jar" -d out $(find src/main/java -name "*.java")
```
- compiles both and you can find the class files in `/out`

- Running the server `java -cp "out:lib/gson-2.10.1.jar" server.app.ServerMain`
- Running the client `java -cp "out:lib/gson-2.10.1.jar" client.app.ClientMain`

### Windows

**Scripts**

- `.\scripts\run.ps1` to start the server in the background and see the client's output
- `.\scripts\build.ps1` to compile the client and the server into separate files in `\out`
- Both load separate config files from `/config`

**By hand**

```powershell
Get-ChildItem -Recurse -Filter *.java src\main\java | ForEach-Object { $_.FullName } | Set-Content sources.txt
javac -cp "lib\gson-2.10.1.jar" -d out "@sources.txt"
```

- compiles both and you can find the class files in `/out`

- Running the server `java -cp "out;lib\gson-2.10.1.jar" server.app.ServerMain`
- Running the client `java -cp "out;lib\gson-2.10.1.jar" client.app.ClientMain`

---

## 2. Design Choices

**Game identity & lifecycle**
- Games are derived purely from time in `GameClockImpl`, there is no "create game" step. `gameId = now / gameDuration`
- Games loop around the available word group pool, so a finite pool of games supports infinite.

**What the server stores vs. computes**
- The server only stores raw player guesses, every stat (score, mistakes, wins/losses) is calculated starting from those, (`ScoreCalculator`, `GameRules`) never stored directly.
- No aggregate stats either (average score, winner count).
- `GameRules` determines a win/loss (among other things) and is used by both client and server.

**Client statelessness**
- The only state the client stores is its `AccountToken`, everything else is derived from server responses each time.
- `GameInfo` sends raw data only (word list, player's correct/wrong guesses).

**Authentication**
- Access tokens are JWTs with the username embedded in the payload, no server side session lookup.
- Stateless so any thread can work on a user's request.

**Data loading**
- `GameWordGroups` are lazily streamed from the JSON file. 

**Participation tracking**
- A player is considered playing the game when he requests `GameInfo` or submits a `Proposal`. A player is also considered playing when logging in.

**Code-shape conventions**
- Interfaces are implemented by records where possible, classes indicate that there is a mutable state.
- Pure domain logic is handled by static method utility classes (e.g. `GameRules`, `GameLogic`, `ScoreCalculator`)

**Persistence**
- Every server state is stored in memory and periodically (`FilePersistenceService`) saved into a JSON file.

**Game change transition**
- `GameId` is calculated dynamically so no need to have a manual transition, `GameTransitionWatcherImpl` is implemented to monitor this transition explicitly only for the sake of the UDP notification.

---

## 3. Thread Schema

## Server-Side Threads

- **Main/Selector** thread
- **Worker pool**
- **Game transition watcher** (daemon) thread
- **Periodic snapshot** (daemon) thread

**Jobs**
- Main/Selector thread -> accepts connections, reads/writes sockets -> submits parsed requests to -> Worker Pool
- Worker Pool threads -> execute request logic (auth, game logic, repository access) -> hand response back to -> Main/Selector thread (writes response bytes)
- Game-Transition Watcher (daemon) -> runs independently, polls game clock -> triggers -> Notification Service (UDP)
- Persistence Snapshot thread (daemon) -> runs independently on a timer -> reads repositories -> writes snapshot files
- Shutdown Hook thread -> runs once at JVM exit -> stops snapshot scheduler, shuts down worker pool, closes server socket, saves final snapshot

**`submitProposal` logic example**
1. selector detects readable socket, reads bytes
2. selector submits parsed request to worker pool
3. worker fullfills the request and possibly modifies game state
4. worker queues response bytes on the connection
5. selector detects writable socket, flushes response

---

## Client-Side Threads

- **Main/UI** thread
- **UDP notification listener** (deamon) thread

**Jobs**
- Main/UI thread -> sends requests synchronously (blocking call) → waits for → response on same thread
- UDP Notification Listener thread (daemon) → blocks waiting for packets → on receipt, signals → shared flag/state object
- Main/UI thread → polls shared flag/state object each loop iteration → reacts if signaled

**game-end UDP notification logic example**
1. server sends UDP packet on game transition
2. listener thread unblocks from receive(), deserializes packet
3. listener signals a shared flag/state object, does not touch UI directly
4. main/UI thread polls the flag on its own loop iteration and renders the result

---

## 4. Data Structures

**Server-side**
- `Account` - username, password hash
- `PlayerGame` - username, gameId, immutable list of `Proposal`s
- `PlayerGameKey` - composite (username, gameId) map key
- `Proposal` - the 4 submitted words + correctness outcome
- `GameWordGroups` / `WordGroup` - theme + words
- Repositories: `ConcurrentHashMap<String, Account>` (accounts), `ConcurrentHashMap<PlayerGameKey, PlayerGame>` (player games), `ConcurrentHashMap<String, InetSocketAddress>` (UDP notification registry)

**Client-side**
- `AccountSession` - holds only the token
- `GameInfoData` - raw server response (words, this player's correct/wrong guess sets, expiry)
- `ApiRequest` / `ApiResponse<T>` / `ApiError` - shared generic envelope types used for every operation

---

## 5. Synchronization Primitives

- **`ConcurrentHashMap`** a lot, for all three in memory repositories (account, player games, UDP notification registry)
- **Per-username locks** in `InMemoryPlayerGameRepository` (`ConcurrentHashMap<String, Lock>`) - guards read-modify-write sequences (`findOrCreate`, `save`) on a single player's game state
- **Ordered dual-lock acquisition** in `updateUsername` (locks acquired in a fixed lexicographic order across two usernames)
- **Double-checked locking + `AtomicInteger`** in `FileGameRepository.getTotalGames()` - cheap volatile read on the common path, `synchronized` block only on first computation of the (immutable-after-first-load) game count.
- **`synchronized` method** a lot
