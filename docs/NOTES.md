
# Client

Client should only keep track of AccountToken and everything else should be derived from the server, not stored or saved.
Score, mistakes and other stats should follow from a function which calculates them.
UDP port to send notifications should be sent to the server on login.

# Server

Game ids and games in general are implicit based on what time it is, no need to keep track of metadata.
Games should loop around if word groups are not enough.
The server only stores players guesses and calculates the current state given a function (for example mistake(guesses, correct_groups) returns the amount of mistakes given a player guesses).
AccessToken should be a JWT, so username should be clear from the payload.
And an access token should be provided each request that requires login.
GameWordGroups should not be all loaded into memory, but be loaded with a lazy json iterator when a game is needed.
The server should NOT save game information like how many players won or average score. Those values should be calculated from the PlayerGames saved data, which is just of guesses from players for a certain game, score and other values will be calculated from a function starting from that data.
Players should be ranked in leaderboard based on score.
PlayerGames data is basically a set of guesses from the player for that game.
GameWordGroups are loaded from a json file which is an array of objects with gameId and groups (which is an array of objects with "theme" and "words" as keys).
There should be two types of response to the GameInfo, normal and for expired games which also gives the correct groups alongside other fields.
Game duration should be a config parameter.
PlayerCredentials and PlayerGames data should persist on restarts, so data should be queued for saving after each game ends, they should be in memory at all times, but saved to a JSON file after each game end.
When a player performs any operation which gives them information about the game (included login), they should be considered playing the game and an empty entry in PlayerGames should be created (just as if they entered the game but not guessed anything yet)
As a response to GameInfo it should not send everything, just enough information for the client to compute the values it needs itself, which means their current guesses and they should calculate score, mistakes etc.
