package server.service;

import server.UserManager;
import server.game.GameClock;
import server.game.GameSession;
import server.game.PlayerProgress;
import server.game.PlayerProgressStore;
import server.game.PuzzleBank;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class FinalizerService {
    private final PlayerProgressStore progressStore;
    private final UserManager userManager;
    private final PuzzleBank puzzleBank;
    private final GameClock clock;
    private final Duration gameDuration;
    private final int maxMistakes;

    public FinalizerService(PlayerProgressStore progressStore, UserManager userManager,
                            PuzzleBank puzzleBank, GameClock clock,
                            Duration gameDuration, int maxMistakes, int intervalSeconds) {
        this.progressStore = progressStore;
        this.userManager = userManager;
        this.puzzleBank = puzzleBank;
        this.clock = clock;
        this.gameDuration = gameDuration;
        this.maxMistakes = maxMistakes;

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "finalizer");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(this::finalizePastGames, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private void finalizePastGames() {
        long currentGameId = clock.currentGameId();
        Map<Long, Map<String, PlayerProgress>> snapshot = progressStore.snapshot();
        for (Map.Entry<Long, Map<String, PlayerProgress>> entry : snapshot.entrySet()) {
            long gameId = entry.getKey();
            if (gameId >= currentGameId) continue;

            GameSession game = reconstructGame(gameId);
            Instant now = clock.now();
            for (Map.Entry<String, PlayerProgress> playerEntry : entry.getValue().entrySet()) {
                String username = playerEntry.getKey();
                PlayerProgress progress = playerEntry.getValue();
                GameSession.Status status = game.playerStatus(progress, now);
                if (status != GameSession.Status.IN_PROGRESS) {
                    userManager.recordCompletedGame(username, gameId,
                            (int) progress.mistakesMade(), (int) progress.solvedCount());
                }
            }
        }
    }

    private GameSession reconstructGame(long gameId) {
        var groups = puzzleBank.getPuzzleForGameId(gameId);
        Instant startTime = clock.startTimeForGameId(gameId);
        return new GameSession(gameId, groups, startTime, gameDuration, maxMistakes);
    }
}