# Architecture.md

# Client

## Data

### Static
- AccountToken?

### Derived
- Any game information (start time, duration, word groups, words already guessed, mistakes etc)
- Any game stats (players in game, finished etc)
- Leaderboards
- Player stats

## Transformations
- Login(username, password) -> AccountToken
- Register(username, password)
- UpdateCredentials(old_username, old_password, new_username, new_password)
- Logout() -> AccountToken
- SubmitProposal(AccountToken, word_group) -> GameState
- RequestGameInfo(AccountToken, game_id) -> GameInfo
- RequestGameStats(AccountToken, game_id) -> GameStats
- RequestLeaderboard(AccountToken, K_users) -> Leaderboard
- RequestLeaderboard(AccountToken, username) -> LeaderboardPosition
- RequestPlayerStats(AccountToken) -> PlayerStats

# Server

## Data

### Static
- GameWordGroups
- PlayerCredentials
- PlayerGames (Map(username, game_id) -> PlayerGame)

### Derived
- IndividualGameStats
- OverallStats
- Leaderboard

## Transformations
- CreateUser(username, password) -> PlayerCredentials
- LoginUser(username, password) -> GetToken(username, password) -> AccountToken
- UpdateCredentials(old_credentials, new_credentials) -> PlayerCredentials, PlayerGames
- GameInfo(game_id) -> GetGameState(PlayerGames, username, game_id) -> GameInfo
- EvaluateGameState(groups_guessed, game_solution) -> GameState
- GameStats(game_id) -> GetGameStats(PlayerGames, game_id) -> GameStats
- GetTopLeaderboard(PlayerGames) -> Leaderboard
- GetPlayerStats(PlayerGames, username) -> PlayerStats