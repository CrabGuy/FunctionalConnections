package shared;

import server.GameManager;
import server.UserManager;

import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "operation"
)
public sealed interface Request {

    String operation();

    Response handle(GameManager gameManager, UserManager userManager, String currentUser);

    record Register(String operation, String username, String psw) implements Request {
        @Override
        public Response handle(GameManager gameManager, UserManager userManager, String currentUser) {
            return Optional.ofNullable(username)
                    .filter(u -> !u.isBlank() && psw != null && !psw.isBlank())
                    .map(u -> userManager.register(username, psw)
                            ? new Response(true, "User " + username + " registered successfully", null)
                            : new Response(false, null, "USERNAME_ALREADY_EXISTS"))
                    .orElseGet(() -> new Response(false, null, "INVALID_CREDENTIALS_FORMAT"));
        }
    }

    record UpdateCredentials(String operation, String oldUsername, String oldPsw, String newUsername, String newPsw) implements Request {
        @Override
        public Response handle(GameManager gameManager, UserManager userManager, String currentUser) {
            return Optional.ofNullable(oldUsername)
                    .filter(u -> currentUser == null || currentUser.equals(u))
                    .map(u -> userManager.updateCredentials(oldUsername, oldPsw, newUsername, newPsw)
                            ? new Response(true, "Credentials updated successfully", null)
                            : new Response(false, null, "INVALID_CREDENTIALS_OR_TARGET_USERNAME_TAKEN"))
                    .orElseGet(() -> new Response(false, null, "UNAUTHORIZED_OR_USER_MISMATCH"));
        }
    }

    record Login(String operation, String username, String psw) implements Request {
        @Override
        public Response handle(GameManager gameManager, UserManager userManager, String currentUser) {
            return Optional.ofNullable(username)
                    .filter(u -> userManager.authenticate(username, psw))
                    .map(u -> new Response(true, "Login successful", null))
                    .orElseGet(() -> new Response(false, null, "INVALID_USERNAME_OR_PASSWORD"));
        }
    }

    record Logout(String operation) implements Request {
        @Override
        public Response handle(GameManager gameManager, UserManager userManager, String currentUser) {
            return Optional.ofNullable(currentUser)
                    .map(u -> new Response(true, "Logout successful", null))
                    .orElseGet(() -> new Response(false, null, "USER_NOT_LOGGED_IN"));
        }
    }

    record SubmitProposal(String operation, List<String> words) implements Request {
        @Override
        public Response handle(GameManager gameManager, UserManager userManager, String currentUser) {
            if (currentUser == null) {
                return new Response(false, null, "USER_NOT_LOGGED_IN");
            }

            var activeGame = gameManager.getActiveGame();
            Set<String> validGameWords = activeGame.wordGroups().stream()
                    .flatMap(group -> group.words().stream())
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());

            return Optional.ofNullable(words)
                    .filter(w -> w.size() == 4 && w.stream().distinct().count() == 4)
                    .map(w -> w.stream().map(String::toUpperCase).toList())
                    .filter(validGameWords::containsAll)
                    .map(HashSet::new)
                    .flatMap(guessSet -> gameManager.processGuess(activeGame.id(), currentUser, guessSet))
                    .map(game -> {
                        var status = gameManager.getPlayerStatus(game, currentUser);
                        var progress = game.playerStates().get(currentUser);
                        boolean lastGuessCorrect = progress.history().get(progress.history().size() - 1).isCorrect();

                        userManager.updateStats(currentUser, (int) progress.mistakesMade(), (int) progress.solvedCount());

                        return new Response(true, "STATUS: " + status + " | LAST GUESS CORRECT: " + lastGuessCorrect, null);
                    })
                    .orElseGet(() -> {
                        boolean allValid = Optional.ofNullable(words)
                                .map(w -> w.stream().map(String::toUpperCase).allMatch(validGameWords::contains))
                                .orElse(false);

                        return allValid
                                ? new Response(false, null, "MALFORMED_PROPOSAL_OR_GAME_OVER")
                                : new Response(false, null, "INVALID_WORDS_NOT_IN_PUZZLE");
                    });
        }
    }

    record RequestGameInfo(String operation, Long gameId) implements Request {
        @Override
        public Response handle(GameManager gameManager, UserManager userManager, String currentUser) {
            long targetId = Optional.ofNullable(gameId).orElseGet(gameManager::getCurrentGameId);

            return gameManager.getGame(targetId)
                    .or(() -> targetId == gameManager.getCurrentGameId() ? Optional.of(gameManager.getActiveGame()) : Optional.empty())
                    .map(game -> {
                        List<String> wordsList = game.wordGroups().stream()
                                .flatMap(group -> group.words().stream())
                                .distinct()
                                .collect(Collectors.toList());

                        Collections.shuffle(wordsList, new Random(game.id()));

                        String formattedWords = String.join(", ", wordsList);
                        long remainingTimeMs = gameManager.getRemainingTime(game).toMillis();

                        return new Response(true, "GAME_ID: " + game.id() + "\nREMAINING_TIME_MS: " + remainingTimeMs + "\nWORDS: " + formattedWords, null);
                    })
                    .orElseGet(() -> new Response(false, null, "GAME_NOT_FOUND"));
        }
    }

    record RequestGameStats(String operation, Long gameId) implements Request {
        @Override
        public Response handle(GameManager gameManager, UserManager userManager, String currentUser) {
            long targetId = Optional.ofNullable(gameId).orElseGet(gameManager::getCurrentGameId);

            return gameManager.getGame(targetId)
                    .map(game -> {
                        var status = gameManager.getPlayerStatus(game, currentUser);
                        var progress = game.playerStates().getOrDefault(currentUser, new GameManager.PlayerProgress(List.of()));
                        return new Response(true, "STATUS:" + status + ",SOLVED:" + progress.solvedCount() + ",MISTAKES:" + progress.mistakesMade(), null);
                    })
                    .orElseGet(() -> new Response(false, null, "GAME_NOT_FOUND"));
        }
    }

    record RequestLeaderboard(String operation, String playerName, Integer topPlayers) implements Request {
        @Override
        public Response handle(GameManager gameManager, UserManager userManager, String currentUser) {
            return Optional.ofNullable(playerName)
                    .filter(userManager::usernameExists)
                    .map(name -> new Response(true, "POSITION:" + userManager.getPosition(name), null))
                    .orElseGet(() -> Optional.ofNullable(topPlayers)
                            .map(k -> k <= 0 ? userManager.getLeaderboard().toList() : userManager.getTopK(k))
                            .map(list -> list.stream().map(u -> u.username + ":" + u.getWins()).collect(Collectors.joining(",")))
                            .map(data -> new Response(true, data, null))
                            .orElseGet(() -> new Response(false, null, "PLAYER_NOT_FOUND_OR_INVALID_PARAMETERS")));
        }
    }

    record RequestPlayerStats(String operation) implements Request {
        @Override
        public Response handle(GameManager gameManager, UserManager userManager, String currentUser) {
            return Optional.ofNullable(currentUser)
                    .map(userManager::get)
                    .map(user -> new Response(true, "USER:" + user.username + ",WINS:" + user.getWins() + ",CURRENT_STREAK:" + user.currentStreak + ",MAX_STREAK:" + user.maxStreak, null))
                    .orElseGet(() -> new Response(false, null, "USER_NOT_LOGGED_IN"));
        }
    }
}