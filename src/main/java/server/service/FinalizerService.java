package server.service;

import server.UserManager;
import server.game.GameRepository;
import server.game.GameClock;
import server.game.GameSession;
import server.game.PlayerProgress;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class FinalizerService {
    private final GameRepository gameRepository;
    private final UserManager userManager;
    private final GameClock clock;

    public FinalizerService(GameRepository gameRepository, UserManager userManager, GameClock clock, int intervalSeconds) {
        this.gameRepository = gameRepository;
        this.userManager = userManager;
        this.clock = clock;
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "finalizer");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(this::finalizePastGames, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private void finalizePastGames() {
        long currentGameId = clock.currentGameId();
        for (long gameId : gameRepository.snapshot().keySet()) {
            if (gameId < currentGameId) {
                GameSession game = gameRepository.findGame(gameId).orElse(null);
                if (game == null) continue;
                Set<String> participants = gameRepository.participantsFor(gameId);
                Instant now = Instant.now();
                for (String username : participants) {
                    var progressOpt = gameRepository.getProgress(gameId, username);
                    if (progressOpt.isPresent()) {
                        PlayerProgress progress = progressOpt.get();
                        GameSession.Status status = game.playerStatus(progress, now);
                        if (status != GameSession.Status.IN_PROGRESS) {
                            userManager.recordCompletedGame(username, gameId,
                                    (int) progress.mistakesMade(), (int) progress.solvedCount());
                        }
                    }
                }
            }
        }
    }
}