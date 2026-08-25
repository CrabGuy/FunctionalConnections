package server;

import shared.ErrorCode;
import shared.Request;
import shared.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public final class RequestDispatcher {
    private final GameManager gameManager;
    private final UserManager userManager;

    public RequestDispatcher(GameManager gameManager, UserManager userManager) {
        this.gameManager = gameManager;
        this.userManager = userManager;
    }

    public Response dispatch(Request request, String currentUser) {
        if (request == null || request.operation() == null) {
            return Response.error(ErrorCode.INVALID_REQUEST);
        }
        return switch (request) {
            case Request.Signup r -> handleSignup(r);
            case Request.Login r -> handleLogin(r);
            case Request.Logout r -> handleLogout(currentUser);
            case Request.UpdateCredentials r -> handleUpdateCredentials(r, currentUser);
            case Request.SendAnswer r -> handleSendAnswer(r, currentUser);
            case Request.RequestGameState r -> handleGameState(r, currentUser);
            case Request.RequestGameStatistics r -> handleGameStatistics(r, currentUser);
            case Request.RequestLeaderboardInfo r -> handleLeaderboard(r, currentUser);
            case Request.RequestPersonalStats r -> handlePersonalStats(currentUser);
        };
    }

    private Response handleSignup(Request.Signup req) {
        if (req.username() == null || req.username().isBlank() || req.psw() == null || req.psw().isBlank()) {
            return Response.error(ErrorCode.INVALID_CREDENTIALS_FORMAT);
        }
        return userManager.register(req.username(), req.psw())
                ? Response.success("User registered successfully")
                : Response.error(ErrorCode.USERNAME_ALREADY_EXISTS);
    }

    private Response handleLogin(Request.Login req) {
        if (req.username() == null || req.psw() == null) {
            return Response.error(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!userManager.authenticate(req.username(), req.psw())) {
            return Response.error(ErrorCode.INVALID_USERNAME_OR_PASSWORD);
        }
        GameManager.Game game = gameManager.getActiveGame();
        recordCompletedGameIfEnded(game, req.username());
        return Response.success("Login successful\n" + buildGameState(game, req.username()));
    }

    private Response handleLogout(String currentUser) {
        return currentUser == null ? Response.error(ErrorCode.USER_NOT_LOGGED_IN) : Response.success("Logout successful");
    }

    private Response handleUpdateCredentials(Request.UpdateCredentials req, String currentUser) {
        if (req.oldUsername() == null || req.oldUsername().isBlank()) {
            return Response.error(ErrorCode.INVALID_USERNAME);
        }
        if (currentUser != null && !currentUser.equals(req.oldUsername())) {
            return Response.error(ErrorCode.UNAUTHORIZED_OR_USER_MISMATCH);
        }
        return switch (userManager.updateCredentials(req.oldUsername(), req.oldPsw(), req.newUsername(), req.newPsw())) {
            case SUCCESS -> Response.success("Credentials updated successfully");
            case INVALID_CREDENTIALS -> Response.error(ErrorCode.INVALID_CREDENTIALS);
            case TARGET_USERNAME_TAKEN -> Response.error(ErrorCode.TARGET_USERNAME_TAKEN);
        };
    }

    private Response handleSendAnswer(Request.SendAnswer req, String currentUser) {
        if (currentUser == null) return Response.error(ErrorCode.USER_NOT_LOGGED_IN);
        if (req.words() == null) return Response.error(ErrorCode.MALFORMED_PROPOSAL);

        List<String> normalized = req.words().stream()
                .map(String::trim)
                .filter(w -> !w.isBlank())
                .map(String::toUpperCase)
                .toList();

        if (normalized.size() != 4 || normalized.stream().distinct().count() != 4) {
            return Response.error(ErrorCode.MALFORMED_PROPOSAL);
        }

        GameManager.Game game = gameManager.getActiveGame();
        if (game.playerStatus(currentUser) != GameManager.Status.IN_PROGRESS) {
            return Response.error(ErrorCode.MALFORMED_PROPOSAL_OR_GAME_OVER);
        }

        if (!game.allWords().containsAll(normalized)) {
            return Response.error(ErrorCode.INVALID_WORDS_NOT_IN_PUZZLE);
        }

        if (normalized.stream().anyMatch(game.solvedWords(currentUser)::contains)) {
            return Response.error(ErrorCode.WORDS_ALREADY_SOLVED);
        }

        Set<String> guess = new HashSet<>(normalized);
        if (game.progress(currentUser).containsGuess(guess)) {
            return Response.error(ErrorCode.DUPLICATE_PROPOSAL);
        }

        return gameManager.processGuess(game.id(), currentUser, guess)
                .map(newGame -> {
                    boolean lastCorrect = newGame.progress(currentUser).history().getLast().isCorrect();
                    recordCompletedGameIfEnded(newGame, currentUser);
                    return Response.success("STATUS:" + newGame.playerStatus(currentUser) + " | LAST GUESS CORRECT: " + lastCorrect);
                })
                .orElseGet(() -> Response.error(ErrorCode.MALFORMED_PROPOSAL_OR_GAME_OVER));
    }

    private Response handleGameState(Request.RequestGameState req, String currentUser) {
        if (currentUser == null) return Response.error(ErrorCode.USER_NOT_LOGGED_IN);
        return gameManager.resolveGame(req.gameId())
                .map(game -> {
                    recordCompletedGameIfEnded(game, currentUser);
                    return Response.success(buildGameState(game, currentUser));
                })
                .orElseGet(() -> Response.error(ErrorCode.GAME_NOT_FOUND));
    }

    private Response handleGameStatistics(Request.RequestGameStatistics req, String currentUser) {
        if (currentUser == null) return Response.error(ErrorCode.USER_NOT_LOGGED_IN);
        return gameManager.resolveGame(req.gameId())
                .map(game -> {
                    recordCompletedGameIfEnded(game, currentUser);
                    return Response.success(buildGameStatistics(game));
                })
                .orElseGet(() -> Response.error(ErrorCode.GAME_NOT_FOUND));
    }

    private Response handleLeaderboard(Request.RequestLeaderboardInfo req, String currentUser) {
        if (currentUser == null) return Response.error(ErrorCode.USER_NOT_LOGGED_IN);
        if (req.playerName() != null && !req.playerName().isBlank()) {
            if (!userManager.usernameExists(req.playerName())) {
                return Response.error(ErrorCode.PLAYER_NOT_FOUND);
            }
            return Response.success("POSITION:" + userManager.getPosition(req.playerName()));
        }
        int limit = req.topPlayers() == null || req.topPlayers() <= 0 ? Integer.MAX_VALUE : req.topPlayers();
        String data = userManager.getLeaderboard()
                .limit(limit)
                .map(user -> user.username() + ":" + user.getWins())
                .collect(Collectors.joining(","));
        return Response.success(data);
    }

    private Response handlePersonalStats(String currentUser) {
        if (currentUser == null) return Response.error(ErrorCode.USER_NOT_LOGGED_IN);
        User user = userManager.get(currentUser);
        if (user == null) return Response.error(ErrorCode.USER_NOT_FOUND);
        recordCompletedGameIfEnded(gameManager.getActiveGame(), currentUser);
        return Response.success(buildPersonalStats(user));
    }

    private void recordCompletedGameIfEnded(GameManager.Game game, String player) {
        if (game.playerStatus(player) != GameManager.Status.IN_PROGRESS && game.playerStates().containsKey(player)) {
            var progress = game.progress(player);
            userManager.recordCompletedGame(player, game.id(), (int) progress.mistakesMade(), (int) progress.solvedCount());
        }
    }

    private String buildGameState(GameManager.Game game, String player) {
        var progress = game.progress(player);
        var status = game.playerStatus(player);
        StringBuilder builder = new StringBuilder();
        builder.append("GAME_ID:").append(game.id()).append("\n");
        builder.append("STATUS:").append(status).append("\n");
        
        if (status == GameManager.Status.IN_PROGRESS) {
            builder.append("REMAINING_TIME_MS:").append(game.remainingTime().toMillis()).append("\n");
            builder.append("SCORE:").append(progress.solvedCount()).append("\n");
            builder.append("MISTAKES:").append(progress.mistakesMade()).append("\n");
            
            String solved = progress.history().stream()
                    .filter(GameManager.Guess::isCorrect)
                    .map(g -> String.join(",", g.words()))
                    .collect(Collectors.joining(";"));
            builder.append("SOLVED_GROUPS:").append(solved.isEmpty() ? "NONE" : solved).append("\n");
            
            Set<String> solvedWords = game.solvedWords(player);
            List<String> remaining = game.wordGroups().stream()
                    .flatMap(g -> g.words().stream())
                    .distinct()
                    .filter(w -> !solvedWords.contains(w.toUpperCase()))
                    .collect(Collectors.toList());
            Collections.shuffle(remaining, new Random(game.id()));
            builder.append("REMAINING_WORDS:").append(String.join(", ", remaining));
        } else {
            builder.append("SCORE:").append(progress.solvedCount()).append("\n");
            builder.append("CORRECT_PROPOSALS:").append(progress.solvedCount()).append("\n");
            builder.append("MISTAKES:").append(progress.mistakesMade()).append("\n");
            String groups = game.wordGroups().stream()
                    .map(g -> g.category() + ":" + String.join(",", g.words()))
                    .collect(Collectors.joining(" | "));
            builder.append("GROUPS:").append(groups);
        }
        return builder.toString();
    }

    private String buildGameStatistics(GameManager.Game game) {
        List<String> players = new ArrayList<>(game.playerStates().keySet());
        long total = players.size();
        long finished = players.stream().filter(p -> game.playerStatus(p) != GameManager.Status.IN_PROGRESS).count();
        long wins = players.stream().filter(p -> game.playerStatus(p) == GameManager.Status.WON).count();
        
        String base = "GAME_ID:" + game.id() + "\n";
        if (game.remainingTime().isZero()) {
            double avgScore = total == 0 ? 0 : players.stream().mapToInt(p -> (int) game.progress(p).solvedCount()).average().orElse(0);
            return base + "TOTAL_PLAYERS:" + total + "\nFINISHED:" + finished + "\nWINS:" + wins + "\nAVG_SCORE:" + avgScore;
        }
        return base + "REMAINING_TIME_MS:" + game.remainingTime().toMillis() + "\nIN_PROGRESS_PLAYERS:" + (total - finished) + "\nFINISHED:" + finished + "\nWINS:" + wins;
    }

    private String buildPersonalStats(User user) {
        int completed = user.games().size();
        int wins = user.getWins();
        double winRate = completed == 0 ? 0 : wins * 100.0 / completed;
        double lossRate = completed == 0 ? 0 : (completed - wins) * 100.0 / completed;
        String hist = user.getMistakeHistogram().entrySet().stream()
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining(","));
        
        return "PUZZLES_COMPLETED:" + completed +
                "\nWIN_RATE:" + String.format("%.1f", winRate) +
                "\nLOSS_RATE:" + String.format("%.1f", lossRate) +
                "\nCURRENT_STREAK:" + user.currentStreak() +
                "\nMAX_STREAK:" + user.maxStreak() +
                "\nPERFECT_PUZZLES:" + user.getPerfectPuzzles() +
                "\nMISTAKE_HISTOGRAM:" + (hist.isEmpty() ? "NONE" : hist);
    }
}