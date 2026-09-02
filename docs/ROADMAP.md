# Connections Game — Architecture Reference

This document defines module boundaries, responsibilities, and interfaces
for a Java client-server implementation of the Connections word game.
No method bodies — signatures and responsibilities only.

---

## 1. Module boundaries

```
shared/   -> protocol contracts (ApiRequest/ApiResponse/*Data/*Request), no logic
server/   -> game engine, persistence, networking server side
client/   -> CLI, NIO client, UDP listener, networking client side
```

Dependency direction: `client -> shared`, `server -> shared`. `client` and
`server` never depend on each other. `shared` depends on nothing.

---

## 2. Server feature slices

Each slice is one implementation unit. Build order: A → B → C → D, with F
buildable in parallel once repository interfaces exist, and E last.

### Slice A — Account

**Domain record:**
```java
package server.dto;
public record Account(String username, String passwordHash) {}
```

**Component: `TokenSigner`**
- `String sign(String username, long expiresAt)`
- `AccountPrincipal verify(String token)` — throws on invalid signature or expired token

```java
package server.dto;
public record AccountPrincipal(String username, long expiresAt) {}
```

**Component: `AccountService`**
- `RegisterData register(String username, String password)`
- `LoginData login(String username, String password, int udpPort, String remoteAddress)`
- `void logout(String accountToken)`
- `UpdateCredentialsData updateCredentials(String oldUsername, String newUsername, String oldPassword, String newPassword)`
- `AccountPrincipal resolve(String accountToken)` — used by every other service to identify the caller

**Component: `AccountRepository`**
- `Optional<Account> findByUsername(String username)`
- `void save(Account account)`
- `boolean existsByUsername(String username)`

**Component: `NotificationRegistry`**
- `void register(String username, InetSocketAddress udpAddress)`
- `void unregister(String username)`
- `Optional<InetSocketAddress> lookup(String username)`

Populated by `AccountService.login`/`logout`; read by Slice E's `NotificationService`.

Depends on: nothing else server-side.

---

### Slice B — Game timing & catalog

**Existing records, unchanged:** `GameWordGroups`, `WordGroup`.

**Component: `GameClock`** (pure functions, no state)
- `long currentGameId(long nowMillis)`
- `long startedAt(long gameId)`
- `long expiresAt(long gameId)`
- `boolean isCompleted(long gameId, long nowMillis)`

**Component: `GameRepository`**
- `GameWordGroups loadById(long gameId)` — reads from the game-data source
- `boolean exists(long gameId)`

Depends on: nothing else server-side.

---

### Slice C — Proposal submission & per-player game state

**Existing records, unchanged:** `Proposal`, `PlayerGame`.

**Component: `ProposalService`**
- `PlayerGameStatus submitProposal(String accountToken, List<String> words)`
- `GameInfoData getGameInfo(String accountToken, Long gameId)` — nullable `gameId` = current game

**Component: `PlayerGameRepository`**
- `PlayerGame findOrCreate(String username, long gameId)`
- `void save(PlayerGame playerGame)`
- `List<PlayerGame> findByGame(long gameId)`
- `List<PlayerGame> findByUsername(String username)`

Depends on: Slice A (`resolve`), Slice B (`GameClock`, `GameRepository`).

---

### Slice D — Stats & Leaderboard

**Component: `StatsService`**
- `GameStatsData getGameStats(Long gameId)` — aggregates over `PlayerGameRepository.findByGame`
- `PlayerStatsData getPlayerStats(String accountToken)` — aggregates over `PlayerGameRepository.findByUsername`

**Component: `LeaderboardService`**
- `LeaderboardData getLeaderboard(String accountToken, String playerName, Integer topK)`

Depends on: Slice A, Slice C.

---

### Slice E — Networking & protocol dispatch

**Component: `RequestDispatcher`**
- `ApiResponse<?> dispatch(ApiRequest request)`

**Component: `ConnectionHandler`**
- NIO channel handler, one per accepted TCP connection, run via thread pool
- reads/writes newline-delimited JSON, deserializes into the concrete `ApiRequest` subtype by `operation`

**Component: `NotificationService`**
- `void notifyGameEnd(GameStatsData result, Collection<String> usernames)` — sends over UDP using `NotificationRegistry` lookups

**Component: `GameTransitionWatcher`**
- background thread that periodically compares `GameClock.currentGameId(now)` to the last observed id
- on change, gathers active participants and calls `NotificationService.notifyGameEnd`
- owns no game state itself — purely a clock-driven trigger

Depends on: all service slices (A–D), for their public interfaces only.

---

### Slice F — Persistence

**Component: `PersistenceService`**
- `void schedulePeriodicSnapshot()` — background task, writes `Account` and `PlayerGame` records to the configured storage directory as JSON
- `void loadOnStartup()` — restores `AccountRepository` and `PlayerGameRepository` state

Uses the repository interfaces from Slices A and C.

---

## 3. Client components

- `ClientConfig`, `ClientGameState` — existing, unchanged.
- **`ConnectionManager`** — `ApiResponse<?> send(ApiRequest request)`, blocking request/response over the persistent TCP NIO channel.
- **`NotificationListener`** — own thread, binds a UDP socket at the port advertised via `LoginRequest`, deserializes incoming game-end notifications, updates `ClientGameState`.
- **`CommandLineInterface`** — parses user commands into the matching `*Request` record, calls `ConnectionManager.send`, prints results.

Depends only on `shared`.

---

## 4. Threading model

- **Server:** one NIO acceptor thread handing channels to a bounded thread
  pool; one background thread for `GameTransitionWatcher`; one background
  thread for `PersistenceService` snapshots. All repositories must be
  thread-safe (synchronized wrappers or concurrent collections, not plain
  `HashMap`).
- **Client:** main thread runs the CLI loop and blocking TCP calls; a
  second thread runs `NotificationListener`.

---

## 8. IMPORTANT

Write signatures + Javadoc only
(behavior, exceptions thrown) — no method bodies. If a slice
needs a record that doesn't exist yet, it should be flagged rather than
invented silently.
