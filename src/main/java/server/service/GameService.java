package server.service;

import server.game.*;
import shared.DataContracts;
import shared.ErrorCode;
import shared.Request;
import shared.Response;
import java.time.Instant;
import java.util.*;

public final class GameService {
    private final GameRepository gameRepository;
    private final GameClock clock;

    public GameService(GameRepository gameRepository, GameClock clock) {
        this.gameRepository = gameRepository;
        this.clock = clock;
    }

    public void ensureAutoParticipation(String username) {
        if (username != null && !username.isBlank()) {
            gameRepository.ensureParticipation(username, clock.currentGameId());
        }
    }

    public Response<DataContracts.GameStateDto> joinGame(String username) {
        ensureAutoParticipation(username);
        GameSession game = gameRepository.getActiveGame();
        return Response.success(buildGameStateDto(gameRepository, game, username, clock));
    }

    public Response<DataContracts.ProposalOutcomeDto> submitProposal(String currentUser, Request.SubmitProposal request) {
        if (currentUser == null) return Response.error(ErrorCode.USER_NOT_LOGGED_IN);
        if (request.words() == null) return Response.error(ErrorCode.MALFORMED_PROPOSAL);
        List<String> normalized = request.words().stream()
                .map(String::trim)
                .filter(w -> !w.isBlank())
                .map(String::toUpperCase)
                .toList();
        if (normalized.size() != 4) return Response.error(ErrorCode.PROPOSAL_WRONG_SIZE);
        if (normalized.stream().distinct().count() != 4) return Response.error(ErrorCode.PROPOSAL_DUPLICATE_WORDS);
        GameSession game = gameRepository.getActiveGame();
        if (!game.allWords().containsAll(normalized)) return Response.error(ErrorCode.PROPOSAL_NOT_IN_PUZZLE);
        Set<String> guess = new HashSet<>(normalized);
        GameRepository.SubmitGuessResult result = gameRepository.submitGuess(game.id(), currentUser, guess);
        if (result.error() != null) {
            return Response.error(result.error());
        }
        PlayerProgress newProgress = result.progress().get();
        boolean lastCorrect = newProgress.history().getLast().isCorrect();
        GameSession.Status newStatus = game.playerStatus(newProgress, clock.now());
        return Response.success(new DataContracts.ProposalOutcomeDto(newStatus.name(), lastCorrect));
    }

    public Response<DataContracts.GameStateDto> getGameState(String currentUser, Long gameId) {
        if (currentUser == null) return Response.error(ErrorCode.USER_NOT_LOGGED_IN);
        Optional<GameSession> gameOpt = gameRepository.findGame(gameId);
        if (gameOpt.isEmpty()) return Response.error(ErrorCode.GAME_NOT_FOUND);
        return Response.success(buildGameStateDto(gameRepository, gameOpt.get(), currentUser, clock));
    }

    public Response<DataContracts.GameStatsDto> getGameStats(String currentUser, Long gameId) {
        if (currentUser == null) return Response.error(ErrorCode.USER_NOT_LOGGED_IN);
        Optional<GameSession> gameOpt = gameRepository.findGame(gameId);
        if (gameOpt.isEmpty()) return Response.error(ErrorCode.GAME_NOT_FOUND);
        GameSession game = gameOpt.get();
        Instant now = clock.now();
        List<String> players = new ArrayList<>(gameRepository.participantsFor(game.id()));
        long total = players.size();
        long finished = 0;
        long wins = 0;
        double scoreSum = 0;
        for (String player : players) {
            var progressOpt = gameRepository.getProgress(game.id(), player);
            if (progressOpt.isPresent()) {
                var progress = progressOpt.get();
                var status = game.playerStatus(progress, now);
                scoreSum += progress.score();
                if (status != GameSession.Status.IN_PROGRESS) finished++;
                if (status == GameSession.Status.WON) wins++;
            }
        }
        if (game.remainingTime(now).isZero()) {
            double avgScore = total == 0 ? 0 : scoreSum / total;
            return Response.success(new DataContracts.GameStatsDto(game.id(), 0L, total, 0, finished, wins, avgScore));
        }
        return Response.success(new DataContracts.GameStatsDto(game.id(), game.remainingTime(now).toMillis(),
                total, total - finished, finished, wins, null));
    }

    public static DataContracts.GameStateDto buildGameStateDto(GameRepository repo, GameSession game, String username, GameClock clock) {
        Instant now = clock.now();
        PlayerProgress progress = repo.getProgress(game.id(), username).orElse(new PlayerProgress(List.of()));
        PlayerGameState state = new PlayerGameState(game, progress, now);
        if (state.getStatus() == GameSession.Status.IN_PROGRESS) {
            return new DataContracts.OngoingGameStateDto(
                    game.id(),
                    state.getStatus().name(),
                    state.getRemainingTimeMs(),
                    state.getScore(),
                    state.getMistakes(),
                    state.getSolvedGroups(),
                    state.getRemainingWords()
            );
        } else {
            return new DataContracts.CompletedGameStateDto(
                    game.id(),
                    state.getStatus().name(),
                    state.getScore(),
                    state.getMistakes(),
                    state.getSolvedGroups(),
                    state.getAllGroups()
            );
        }
    }
}