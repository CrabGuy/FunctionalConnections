package server.service;

import server.game.*;
import server.game.GameSession.WordGroup;
import shared.DataContracts;
import shared.ErrorCode;
import shared.Request;
import shared.Response;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public final class GameService {
    private final GameRepository gameRepository;
    private final GameClock clock;

    public GameService(GameRepository gameRepository, GameClock clock) {
        this.gameRepository = gameRepository;
        this.clock = clock;
    }

    public Response<DataContracts.GameStateDto> joinGame(String username) {
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
        if (normalized.size() != 4 || normalized.stream().distinct().count() != 4) {
            return Response.error(ErrorCode.MALFORMED_PROPOSAL);
        }
        GameSession game = gameRepository.getActiveGame();
        Instant now = Instant.now();
        var progressOpt = gameRepository.getProgress(game.id(), currentUser);
        var status = game.playerStatus(progressOpt.orElse(new PlayerProgress(List.of())), now);
        if (status != GameSession.Status.IN_PROGRESS) {
            return Response.error(ErrorCode.MALFORMED_PROPOSAL_OR_GAME_OVER);
        }
        if (!game.allWords().containsAll(normalized)) {
            return Response.error(ErrorCode.INVALID_WORDS_NOT_IN_PUZZLE);
        }
        if (progressOpt.isPresent() && normalized.stream().anyMatch(progressOpt.get().history().stream()
                .filter(PlayerProgress.Guess::isCorrect)
                .flatMap(g -> g.words().stream())
                .collect(Collectors.toSet())::contains)) {
            return Response.error(ErrorCode.WORDS_ALREADY_SOLVED);
        }
        Set<String> guess = new HashSet<>(normalized);
        if (progressOpt.isPresent() && progressOpt.get().containsGuess(guess)) {
            return Response.error(ErrorCode.DUPLICATE_PROPOSAL);
        }
        Optional<PlayerProgress> newProgress = gameRepository.submitGuess(game.id(), currentUser, guess);
        if (newProgress.isEmpty()) {
            return Response.error(ErrorCode.MALFORMED_PROPOSAL_OR_GAME_OVER);
        }
        boolean lastCorrect = newProgress.get().history().getLast().isCorrect();
        GameSession.Status newStatus = game.playerStatus(newProgress.get(), now);
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
        Instant now = Instant.now();
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
        Instant now = Instant.now();
        var progressOpt = repo.getProgress(game.id(), username);
        var progress = progressOpt.orElse(new PlayerProgress(List.of()));
        var status = game.playerStatus(progress, now);
        List<DataContracts.SolvedGroupDto> solvedGroups = progress.history().stream()
                .filter(PlayerProgress.Guess::isCorrect)
                .map(g -> {
                    String category = game.wordGroups().stream()
                            .filter(wg -> wg.words().equals(g.words()))
                            .map(WordGroup::category)
                            .findFirst()
                            .orElse("Unknown");
                    return new DataContracts.SolvedGroupDto(category, g.words());
                })
                .toList();
        if (status == GameSession.Status.IN_PROGRESS) {
            Set<String> solvedWords = progress.history().stream()
                    .filter(PlayerProgress.Guess::isCorrect)
                    .flatMap(g -> g.words().stream())
                    .collect(Collectors.toSet());
            List<String> remaining = game.wordGroups().stream()
                    .flatMap(g -> g.words().stream())
                    .distinct()
                    .filter(w -> !solvedWords.contains(w.toUpperCase()))
                    .collect(Collectors.toList());
            Collections.shuffle(remaining, new Random(game.id()));
            return new DataContracts.GameStateDto(
                    game.id(), status.name(), game.remainingTime(now).toMillis(), progress.score(),
                    progress.mistakesMade(), solvedGroups, remaining, null
            );
        } else {
            List<DataContracts.GameGroupDto> allGroups = game.wordGroups().stream()
                    .map(g -> new DataContracts.GameGroupDto(g.category(), g.words()))
                    .toList();
            return new DataContracts.GameStateDto(
                    game.id(), status.name(), 0L, progress.score(), progress.mistakesMade(),
                    solvedGroups, null, allGroups
            );
        }
    }
}