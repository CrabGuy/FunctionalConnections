# Connections Game - Project Requirements

## 1. Game Description

Connections is a word grouping game where players must find associations between words. The game features:

- 16 words divided into 4 thematic groups of 4 words each
- Words are shuffled and displayed to all players
- Each player can submit grouping proposals (quadruples of words)
- Maximum of 4 wrong proposals per game
- One active game per time period for all players
- Players must wait for the current game period to end before accessing the next game

### Scoring System

| Action | Points |
|--------|--------|
| Participate without proposals | 0 |
| 1 correct proposal | +6 |
| 2 correct proposals | +12 |
| 3 correct proposals (win) | +18 |
| Each wrong proposal | -4 (max -16 per game) |

### Winning Conditions
- Submit 3 correct groups of 4 words (last group is implied by elimination)
- Before time expires
- Before reaching 4 wrong proposals

### Example Scoring Scenarios
- 1 wrong proposal + 1 correct proposal + 1 wrong proposal (time expires): -2 points
- Win with 3 correct proposals and 3 errors: +6 points

---

## 2. Client-Server Architecture

### 2.1 Client Requirements

**User Management Operations:**
- **Registration**: User can register with username and password if username is not already taken
- **Update Credentials**: User can update username or password if they know the current password
- **Login**: User can login with correct password; automatically joins current game
- **Logout**: Stops receiving notifications and submitting proposals

**Game Management Operations (logged in only):**

1. **Submit Proposal**: Send 4-word proposal from remaining ungrouped words
   - Server responds with correctness notification

2. **Request Game Status/Outcome** (by game ID or current game):
   - **Ongoing game**: Remaining time, correct proposals, remaining words, error count, current score
   - **Completed game**: Correct word-to-group assignment, correct proposals count, error count, score

3. **Request Game Statistics** (by game ID or current game):
   - **Ongoing game**: Remaining time, active players count, completed players count, winners count
   - **Completed game**: Total participants, completed players, winners, average score

**General Information Operations (logged in only):**

1. **Leaderboard Information**:
   - Full leaderboard for all users
   - Top K users
   - Specific user's position

2. **Personal Statistics**:
   - Puzzles Completed (total)
   - Win Rate (%)
   - Loss Rate (%)
   - Current Streak (consecutive wins)
   - Max Streak (highest consecutive wins)
   - Perfect Puzzles (0 mistakes)
   - Mistake Histogram (0-4 mistakes, failures, incomplete)

**Automatic Participation**: Logged-in players automatically join new games when they start

---

### 2.2 Server Requirements

**Core Responsibilities:**
- Manage the entire game
- Coordinate players
- Enforce game rules
- Provide real-time updates

**Data Source:**
- JSON file containing game data (words and groups)
- 911 games available in the provided file
- Must handle as large-scale (regardless of actual file size)

**Game Lifecycle:**
- Creates new games at fixed time intervals
- One active global game at any time
- Accepts all logged-in players at any point during game duration
- Persists user and game information periodically
- Maintains consistency for server restarts

**Game Management:**
- Waits for players to login
- Sends current game information to players (words, remaining time, without revealing groupings)
- Restores player state if they rejoin during same game
- Players who completed a game must wait for next game

**Proposal Processing:**
- Receives and validates player proposals (4 words)
- Checks if words form a correct and unclaimed group
- Updates game state accordingly (errors, score)
- Malformed proposals (already grouped words, invalid words) count as errors but don't affect game state

**Game Termination:**
- Individual player: wins or loses
- Global: time expires
- Sends game results and rankings to all participants
- Transitions to next game

---

## 3. Implementation Specifications

### Technology Requirements

**Client:**
- Command-line interface (GUI optional, not evaluated)
- TCP connection for registration
- Persistent TCP connection with JSON message format (Section 5)
- NIO for connection management
- UDP support for asynchronous notifications (game end notifications)

**Server:**
- Multithreaded using Java thread pooling
- Synchronized data structures for users and game state
- JSON format for user and game persistence files

### Required Deliverables

**Code Requirements:**
- Compiles from command line using `javac`
- Must be properly commented
- Classes with `main` method must contain "Main" in filename (e.g., `ServerMain.java`)
- JAR executable files for client and server

**Configuration:**
- Parameters (ports, addresses, timeouts, etc.) read from configuration files
- Separate files for client and server
- No interactive input or command-line parameters for configuration

**IDE and Libraries:**
- If using IDE, submit only source code (remove IDE-specific files)
- Include any external libraries used (JAR format)

**Communication Protocol:**
- Client commands must follow Section 5 syntax

### Documentation Requirements (Max 5 pages PDF)

1. **Design Choices**: Explanation of decisions made for open-ended parts of the project
2. **Thread Architecture**: General schema of threads on both client and server
3. **Data Structures**: Definitions for client and server data structures
4. **Synchronization**: Description of synchronization primitives for shared data structures
5. **User Manual**: Clear compilation and execution instructions including:
   - External libraries used
   - Arguments required
   - Command syntax for operations

---

## 4. Submission Instructions

- **Format**: Single ZIP archive (not RAR, not GZ)
- **Platform**: Moodle
- **Contents**: Source code + PDF documentation

---

## 5. Communication Protocol Specifications

All client-server communication uses structured textual JSON messages. Every request must include the specified keys, with optional additional elements allowed.

### Operation Formats

| Operation | Request Format | Requirements |
|-----------|---------------|--------------|
| **register** | `{ "operation": "register", "username": STRING, "psw": STRING }` | Specify error codes (e.g., username already registered) and JSON response format |
| **updateCredentials** | `{ "operation": "updateCredentials", "oldUsername": STRING, "newUsername": STRING, "oldPsw": STRING, "newPsw": STRING }` | Specify error codes (e.g., incorrect old password, new username already taken) and JSON response format |
| **login** | `{ "operation": "login", "username": STRING, "psw": STRING }` | Specify error codes (e.g., incorrect password) and JSON response format |
| **logout** | `{ "operation": "logout" }` | Specify error codes (e.g., user not logged in) and JSON response format |
| **submitProposal** | `{ "operation": "submitProposal", "words": [STRING, ..., STRING] }` | Specify error codes (e.g., malformed proposal) and JSON response format for correct or error responses |
| **requestGameInfo** | `{ "operation": "requestGameInfo", "gameId": INT }` | Specify error codes (e.g., nonexistent game) and JSON response format with Section 2.1 information |
| **requestGameStats** | `{ "operation": "requestGameStats", "gameId": INT }` | Option to specify current game. Specify error codes and JSON response format with Section 2.1 information |
| **requestLeaderboard** | `{ "operation": "requestLeaderboard", "playerName": STRING, "topPlayers": INT }` | Can request relative ranking or top K users. Option to specify all players. Specify error codes and JSON response format with Section 2.1 information |
| **requestPlayersStats** | `{ "operation": "requestPlayersStats" }` | Specify error codes and JSON response format with Section 2.1 information |

### Notes
- The `gameId` field can be used to specify current game
- For `requestLeaderboard`, options available for requesting all players or specific ranking information
- All error codes and detailed response formats must be properly defined