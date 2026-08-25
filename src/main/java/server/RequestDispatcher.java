package server;
import shared.DataContracts;
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
    public Response<?> dispatch(Request request, String currentUser) {
        if (request == null) {
            return Response.error(ErrorCode.INVALID_REQUEST);
        }
        return switch (request) {
            case Request.Register r -> handleSignup(r);
            case Request.Login r -> handleLogin(r);
            case Request.Logout r -> handleLogout(currentUser);
            case Request.UpdateCredentials r -> handleUpdateCredentials(r, currentUser);
            case Request.SubmitProposal r -> handleSubmitProposal(r, currentUser);
            case Request.RequestGameInfo r -> handleGameInfo(r, currentUser);
            case Request.RequestGameStats r -> handleGameStatistics(r, currentUser);
            case Request.RequestLeaderboard r -> handleLeaderboard(r, currentUser);
            case Request.RequestPlayerStats r -> handlePersonalStats(currentUser);
        };
    }
    private Response<Void> handleSignup(Request.Register req) {
        if (req.username() == null || req.username().isBlank() || req.psw() == null || req.psw().isBlank()) {
            return Response.error(ErrorCode.INVALID_CREDENTIALS_FORMAT);
        }
        return userManager.register(req.username(), req.psw())
                ? Response.success(null)
                : Response.error(ErrorCode.USERNAME_ALREADY_EXISTS);
    }
    private Response<DataContracts.GameStateDto> handleLogin(Request.Login req) {
        if (req.username() == null || req.psw() == null) {
            return Response.error(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!userManager.authenticate(req.username(), req.psw())) {
            return Response.error(ErrorCode.INVALID_USERNAME_OR_PASSWORD);
        }
        GameManager.Game game = gameManager.getActiveGame();
        recordCompletedGameIfEnded(game, req.username());
        return Response.success(buildGameStateDto(game, req.username()));
    }
    private Response<Void> handleLogout(String currentUser) {
        return currentUser == null ? Response.error(ErrorCode.USER_NOT_LOGGED_IN) : Response.success(null);
    }
    private Response<Void> handleUpdateCredentials(Request.UpdateCredentials req, String currentUser) {
        if (req.oldUsername() == null || req.oldUsername().isBlank()) {
            return Response.error(ErrorCode.INVALID_USERNAME);
        }
        if (currentUser != null && !currentUser.equals(req.oldUsername())) {
            return Response.error(ErrorCode.UNAUTHORIZED_OR_USER_MISMATCH);
        }
        return switch (userManager.updateCredentials(req.oldUsername(), req.oldPsw(), req.newUsername(), req.newPsw())) {
            case SUCCESS -> Response.success(null);
            case INVALID_CREDENTIALS -> Response.error(ErrorCode.INVALID_CREDENTIALS);
            case TARGET_USERNAME_TAKEN -> Response.error(ErrorCode.TARGET_USERNAME_TAKEN);
        };
    }
    private Response<DataContracts.ProposalOutcomeDto> handleSubmitProposal(Request.SubmitProposal req, String currentUser) {
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
                    return Response.success(new DataContracts.ProposalOutcomeDto(newGame.playerStatus(currentUser).name(), lastCorrect));
                })
                .orElseGet(() -> Response.error(ErrorCode.MALFORMED_PROPOSAL_OR_GAME_OVER));
    }
    private Response<DataContracts.GameStateDto> handleGameInfo(Request.RequestGameInfo req, String currentUser) {
        if (currentUser == null) return Response.error(ErrorCode.USER_NOT_LOGGED_IN);
        return gameManager.resolveGame(req.gameId())
                .map(game -> {
                    recordCompletedGameIfEnded(game, currentUser);
                    return Response.success(buildGameStateDto(game, currentUser));
                })
                .orElseGet(() -> Response.error(ErrorCode.GAME_NOT_FOUND));
    }
    private Response<DataContracts.GameStatsDto> handleGameStatistics(Request.RequestGameStats req, String currentUser) {
        if (currentUser == null) return Response.error(ErrorCode.USER_NOT_LOGGED_IN);
        return gameManager.resolveGame(req.gameId())
                .map(game -> {
                    recordCompletedGameIfEnded(game, currentUser);
                    return Response.success(buildGameStatisticsDto(game));
                })
                .orElseGet(() -> Response.error(ErrorCode.GAME_NOT_FOUND));
    }
    private Response<DataContracts.LeaderboardDto> handleLeaderboard(Request.RequestLeaderboard req, String currentUser) {
        if (currentUser == null) return Response.error(ErrorCode.USER_NOT_LOGGED_IN);
        if (req.playerName() != null && !req.playerName().isBlank()) {
            if (!userManager.usernameExists(req.playerName())) {
                return Response.error(ErrorCode.PLAYER_NOT_FOUND);
            }
            return Response.success(new DataContracts.LeaderboardDto(userManager.getPosition(req.playerName()), List.of()));
        }
        int limit = req.topPlayers() == null || req.topPlayers() <= 0 ? Integer.MAX_VALUE : req.topPlayers();
        List<DataContracts.LeaderboardEntry> entries = userManager.getLeaderboard()
                .limit(limit)
                .map(u -> new DataContracts.LeaderboardEntry(u.username(), u.getWins()))
                .toList();
        return Response.success(new DataContracts.LeaderboardDto(null, entries));
    }
    private Response<DataContracts.PlayerStatsDto> handlePersonalStats(String currentUser) {
        if (currentUser == null) return Response.error(ErrorCode.USER_NOT_LOGGED_IN);
        User user = userManager.get(currentUser);
        if (user == null) return Response.error(ErrorCode.USER_NOT_FOUND);
        recordCompletedGameIfEnded(gameManager.getActiveGame(), currentUser);
        int completed = user.games().size();
        int wins = user.getWins();
        double winRate = completed == 0 ? 0 : wins * 100.0 / completed;
        double lossRate = completed == 0 ? 0 : (completed - wins) * 100.0 / completed;
        String hist = user.getMistakeHistogram().entrySet().stream()
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining(","));
        return Response.success(new DataContracts.PlayerStatsDto(
                completed, winRate, lossRate, user.currentStreak(),
                user.maxStreak(), user.getPerfectPuzzles(), hist.isEmpty() ? "NONE" : hist
        ));
    }
    private void recordCompletedGameIfEnded(GameManager.Game game, String player) {
        if (game.playerStatus(player) != GameManager.Status.IN_PROGRESS && game.playerStates().containsKey(player)) {
            var progress = game.progress(player);
            userManager.recordCompletedGame(player, game.id(), (int) progress.mistakesMade(), (int) progress.solvedCount());
        }
    }
    private DataContracts.GameStateDto buildGameStateDto(GameManager.Game game, String player) {
        var progress = game.progress(player);
        var status = game.playerStatus(player);
        List<Set<String>> solvedGroups = progress.history().stream()
                .filter(GameManager.Guess::isCorrect)
                .map(GameManager.Guess::words)
                .toList();
        if (status == GameManager.Status.IN_PROGRESS) {
            Set<String> solvedWords = game.solvedWords(player);
            List<String> remaining = game.wordGroups().stream()
                    .flatMap(g -> g.words().stream())
                    .distinct()
                    .filter(w -> !solvedWords.contains(w.toUpperCase()))
                    .collect(Collectors.toList());
            Collections.shuffle(remaining, new Random(game.id()));
            return new DataContracts.GameStateDto(
                    game.id(), status.name(), game.remainingTime().toMillis(), progress.score(), progress.mistakesMade(), solvedGroups, remaining, null
            );
        } else {
            List<String> allGroups = game.wordGroups().stream()
                    .map(g -> g.category() + ": " + String.join(", ", g.words()))
                    .toList();
            return new DataContracts.GameStateDto(
                    game.id(), status.name(), 0L, progress.score(), progress.mistakesMade(), solvedGroups, null, allGroups
            );
        }
    }
    private DataContracts.GameStatsDto buildGameStatisticsDto(GameManager.Game game) {
        List<String> players = new ArrayList<>(game.playerStates().keySet());
        long total = players.size();
        long finished = players.stream().filter(p -> game.playerStatus(p) != GameManager.Status.IN_PROGRESS).count();
        long wins = players.stream().filter(p -> game.playerStatus(p) == GameManager.Status.WON).count();
        if (game.remainingTime().isZero()) {
            double avgScore = total == 0 ? 0 : players.stream().mapToLong(p -> game.progress(p).score()).average().orElse(0);
            return new DataContracts.GameStatsDto(game.id(), 0L, total, 0, finished, wins, avgScore);
        }
        return new DataContracts.GameStatsDto(game.id(), game.remainingTime().toMillis(), total, total - finished, finished, wins, 0);
    }
}