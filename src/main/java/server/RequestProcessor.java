package server;

import shared.Request;
import shared.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public final class RequestProcessor {
    private final GameManager gameManager;
    private final UserManager userManager;

    public RequestProcessor(GameManager gameManager, UserManager userManager) {
        this.gameManager = gameManager;
        this.userManager = userManager;
    }

    public Response handle(Request request, String currentUser) {
        if (request == null || request.operation() == null) {
            return error("Invalid request format");
        }
        if (request instanceof Request.Signup signup) return handleSignup(signup);
        if (request instanceof Request.UpdateCredentials update) return handleUpdateCredentials(update, currentUser);
        if (request instanceof Request.Login login) return handleLogin(login);
        if (request instanceof Request.Logout) return handleLogout(currentUser);
        if (request instanceof Request.SendAnswer sendAnswer) return handleSendAnswer(sendAnswer, currentUser);
        if (request instanceof Request.RequestGameState gameState) return handleGameState(gameState, currentUser);
        if (request instanceof Request.RequestGameStatistics stats) return handleGameStatistics(stats, currentUser);
        if (request instanceof Request.RequestLeaderboardInfo leaderboard) return handleLeaderboard(leaderboard, currentUser);
        if (request instanceof Request.RequestPersonalStats personal) return handlePersonalStats(personal, currentUser);
        return error("Unknown request");
    }

    private Response error(String message) {
        return new Response(false, null, message);
    }

    private Response handleSignup(Request.Signup req) {
        if (req.username() == null || req.username().isBlank() || req.psw() == null || req.psw().isBlank()) {
            return error("INVALID_CREDENTIALS_FORMAT");
        }
        boolean registered = userManager.register(req.username(), req.psw());
        return registered
                ? new Response(true, "User registered successfully", null)
                : error("USERNAME_ALREADY_EXISTS");
    }

    private Response handleUpdateCredentials(Request.UpdateCredentials req, String currentUser) {
        if (req.oldUsername() == null || req.oldUsername().isBlank()) {
            return error("INVALID_USERNAME");
        }
        if (currentUser != null && !currentUser.equals(req.oldUsername())) {
            return error("UNAUTHORIZED_OR_USER_MISMATCH");
        }
        boolean updated = userManager.updateCredentials(req.oldUsername(), req.oldPsw(), req.newUsername(), req.newPsw());
        return updated
                ? new Response(true, "Credentials updated successfully", null)
                : error("INVALID_CREDENTIALS_OR_TARGET_USERNAME_TAKEN");
    }

    private Response handleLogin(Request.Login req) {
        if (req.username() == null || req.psw() == null) {
            return error("INVALID_CREDENTIALS");
        }
        if (!userManager.authenticate(req.username(), req.psw())) {
            return error("INVALID_USERNAME_OR_PASSWORD");
        }
        GameManager.Game game = gameManager.getActiveGame();
        recordCompletedGameIfEnded(game, req.username());
        GameManager.Status status = gameManager.getPlayerStatus(game, req.username());
        return new Response(true, "Login successful\n" + buildGameState(game, req.username(), status), null);
    }

    private Response handleLogout(String currentUser) {
        if (currentUser == null) {
            return error("USER_NOT_LOGGED_IN");
        }
        return new Response(true, "Logout successful", null);
    }

    private Response handleSendAnswer(Request.SendAnswer req, String currentUser) {
        if (currentUser == null) {
            return error("USER_NOT_LOGGED_IN");
        }
        if (req.words() == null) {
            return error("MALFORMED_PROPOSAL");
        }
        List<String> normalized = req.words().stream()
                .map(String::trim)
                .filter(word -> !word.isBlank())
                .map(String::toUpperCase)
                .toList();
        if (normalized.size() != 4 || normalized.stream().distinct().count() != 4) {
            return error("MALFORMED_PROPOSAL");
        }

        GameManager.Game game = gameManager.getActiveGame();
        GameManager.Status currentStatus = gameManager.getPlayerStatus(game, currentUser);
        if (currentStatus != GameManager.Status.IN_PROGRESS) {
            return error("MALFORMED_PROPOSAL_OR_GAME_OVER");
        }

        Set<String> allValidWords = allWords(game);
        if (!allValidWords.containsAll(normalized)) {
            return error("INVALID_WORDS_NOT_IN_PUZZLE");
        }

        Set<String> solvedWords = solvedWords(game, currentUser);
        if (normalized.stream().anyMatch(solvedWords::contains)) {
            return error("WORDS_ALREADY_SOLVED");
        }

        Set<String> guess = new HashSet<>(normalized);
        GameManager.PlayerProgress currentProgress = progress(game, currentUser);
        if (currentProgress.containsGuess(guess)) {
            return error("DUPLICATE_PROPOSAL");
        }

        Optional<GameManager.Game> updated = gameManager.processGuess(game.id(), currentUser, guess);
        if (updated.isEmpty()) {
            return error("MALFORMED_PROPOSAL_OR_GAME_OVER");
        }

        GameManager.Game newGame = updated.get();
        GameManager.PlayerProgress newProgress = progress(newGame, currentUser);
        boolean lastCorrect = newProgress.history().get(newProgress.history().size() - 1).isCorrect();
        recordCompletedGameIfEnded(newGame, currentUser);
        GameManager.Status status = gameManager.getPlayerStatus(newGame, currentUser);
        return new Response(true, "STATUS:" + status + " | LAST GUESS CORRECT: " + lastCorrect, null);
    }

    private Response handleGameState(Request.RequestGameState req, String currentUser) {
        if (currentUser == null) {
            return error("USER_NOT_LOGGED_IN");
        }
        long targetId = req.gameId() != null ? req.gameId() : gameManager.getCurrentGameId();
        GameManager.Game game = gameManager.getGame(targetId)
                .or(() -> targetId == gameManager.getCurrentGameId()
                        ? Optional.of(gameManager.getActiveGame())
                        : Optional.empty())
                .orElse(null);
        if (game == null) {
            return error("GAME_NOT_FOUND");
        }
        recordCompletedGameIfEnded(game, currentUser);
        GameManager.Status status = gameManager.getPlayerStatus(game, currentUser);
        return new Response(true, buildGameState(game, currentUser, status), null);
    }

    private Response handleGameStatistics(Request.RequestGameStatistics req, String currentUser) {
        if (currentUser == null) {
            return error("USER_NOT_LOGGED_IN");
        }
        long targetId = req.gameId() != null ? req.gameId() : gameManager.getCurrentGameId();
        GameManager.Game game = gameManager.getGame(targetId)
                .or(() -> targetId == gameManager.getCurrentGameId()
                        ? Optional.of(gameManager.getActiveGame())
                        : Optional.empty())
                .orElse(null);
        if (game == null) {
            return error("GAME_NOT_FOUND");
        }
        recordCompletedGameIfEnded(game, currentUser);
        return new Response(true, buildGameStatistics(game), null);
    }

    private Response handleLeaderboard(Request.RequestLeaderboardInfo req, String currentUser) {
        if (currentUser == null) {
            return error("USER_NOT_LOGGED_IN");
        }
        String playerName = req.playerName();
        if (playerName != null && !playerName.isBlank()) {
            if (!userManager.usernameExists(playerName)) {
                return error("PLAYER_NOT_FOUND");
            }
            return new Response(true, "POSITION:" + userManager.getPosition(playerName), null);
        }
        int limit = req.topPlayers() == null || req.topPlayers() <= 0
                ? Integer.MAX_VALUE
                : req.topPlayers();
        String data = userManager.getLeaderboard()
                .limit(limit)
                .map(user -> user.username + ":" + user.getWins())
                .collect(Collectors.joining(","));
        return new Response(true, data, null);
    }

    private Response handlePersonalStats(Request.RequestPersonalStats req, String currentUser) {
        if (currentUser == null) {
            return error("USER_NOT_LOGGED_IN");
        }
        User user = userManager.get(currentUser);
        if (user == null) {
            return error("USER_NOT_FOUND");
        }
        GameManager.Game activeGame = gameManager.getActiveGame();
        recordCompletedGameIfEnded(activeGame, currentUser);
        return new Response(true, buildPersonalStats(user), null);
    }

    private GameManager.PlayerProgress progress(GameManager.Game game, String player) {
        return game.playerStates().getOrDefault(player, new GameManager.PlayerProgress(List.of()));
    }

    private Set<String> allWords(GameManager.Game game) {
        return game.wordGroups().stream()
                .flatMap(group -> group.words().stream())
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }

    private Set<String> solvedWords(GameManager.Game game, String player) {
        return progress(game, player).history().stream()
                .filter(GameManager.Guess::isCorrect)
                .flatMap(guess -> guess.words().stream())
                .collect(Collectors.toSet());
    }

    private void recordCompletedGameIfEnded(GameManager.Game game, String player) {
        GameManager.Status status = gameManager.getPlayerStatus(game, player);
        if (status != GameManager.Status.IN_PROGRESS && game.playerStates().containsKey(player)) {
            GameManager.PlayerProgress playerProgress = progress(game, player);
            userManager.recordCompletedGame(
                    player,
                    game.id(),
                    (int) playerProgress.mistakesMade(),
                    (int) playerProgress.solvedCount()
            );
        }
    }

    private String buildGameState(GameManager.Game game, String player, GameManager.Status status) {
        GameManager.PlayerProgress playerProgress = progress(game, player);
        StringBuilder builder = new StringBuilder();
        builder.append("GAME_ID:").append(game.id()).append("\n");
        builder.append("STATUS:").append(status).append("\n");
        if (status == GameManager.Status.IN_PROGRESS) {
            builder.append("REMAINING_TIME_MS:").append(gameManager.getRemainingTime(game).toMillis()).append("\n");
            builder.append("SCORE:").append(playerProgress.solvedCount()).append("\n");
            builder.append("MISTAKES:").append(playerProgress.mistakesMade()).append("\n");
            builder.append("SOLVED_GROUPS:").append(formatSolvedGroups(playerProgress)).append("\n");
            builder.append("REMAINING_WORDS:").append(formatRemainingWords(game, player));
        } else {
            builder.append("SCORE:").append(playerProgress.solvedCount()).append("\n");
            builder.append("CORRECT_PROPOSALS:").append(playerProgress.solvedCount()).append("\n");
            builder.append("MISTAKES:").append(playerProgress.mistakesMade()).append("\n");
            builder.append("GROUPS:").append(formatCorrectGroups(game));
        }
        return builder.toString();
    }

    private String formatSolvedGroups(GameManager.PlayerProgress playerProgress) {
        String solved = playerProgress.history().stream()
                .filter(GameManager.Guess::isCorrect)
                .map(guess -> String.join(",", guess.words()))
                .collect(Collectors.joining(";"));
        return solved.isEmpty() ? "NONE" : solved;
    }

    private String formatRemainingWords(GameManager.Game game, String player) {
        Set<String> solved = solvedWords(game, player);
        List<String> remaining = game.wordGroups().stream()
                .flatMap(group -> group.words().stream())
                .distinct()
                .filter(word -> !solved.contains(word.toUpperCase()))
                .collect(Collectors.toList());
        Collections.shuffle(remaining, new Random(game.id()));
        return String.join(", ", remaining);
    }

    private String formatCorrectGroups(GameManager.Game game) {
        return game.wordGroups().stream()
                .map(group -> group.category() + ":" + String.join(",", group.words()))
                .collect(Collectors.joining(" | "));
    }

    private String buildGameStatistics(GameManager.Game game) {
        List<String> players = new ArrayList<>(game.playerStates().keySet());
        long total = players.size();
        long finished = players.stream()
                .filter(player -> gameManager.getPlayerStatus(game, player) != GameManager.Status.IN_PROGRESS)
                .count();
        long wins = players.stream()
                .filter(player -> gameManager.getPlayerStatus(game, player) == GameManager.Status.WON)
                .count();
        if (gameManager.getRemainingTime(game).isZero()) {
            double averageScore = total == 0
                    ? 0
                    : players.stream()
                            .mapToInt(player -> (int) progress(game, player).solvedCount())
                            .average()
                            .orElse(0);
            return "TOTAL_PLAYERS:" + total +
                    "\nFINISHED:" + finished +
                    "\nWINS:" + wins +
                    "\nAVG_SCORE:" + averageScore;
        }
        long inProgress = total - finished;
        return "REMAINING_TIME_MS:" + gameManager.getRemainingTime(game).toMillis() +
                "\nIN_PROGRESS_PLAYERS:" + inProgress +
                "\nFINISHED:" + finished +
                "\nWINS:" + wins;
    }

    private String buildPersonalStats(User user) {
        int completed = user.games.size();
        int wins = user.getWins();
        int losses = completed - wins;
        double winRate = completed == 0 ? 0 : wins * 100.0 / completed;
        double lossRate = completed == 0 ? 0 : losses * 100.0 / completed;
        String histogram = user.getMistakeHistogram().entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(","));
        return "PUZZLES_COMPLETED:" + completed +
                "\nWIN_RATE:" + String.format("%.1f", winRate) +
                "\nLOSS_RATE:" + String.format("%.1f", lossRate) +
                "\nCURRENT_STREAK:" + user.currentStreak +
                "\nMAX_STREAK:" + user.maxStreak +
                "\nPERFECT_PUZZLES:" + user.getPerfectPuzzles() +
                "\nMISTAKE_HISTOGRAM:" + (histogram.isEmpty() ? "NONE" : histogram);
    }
}