package server.network;

import server.dto.PlayerGame;
import server.game.GameClock;
import server.game.PlayerGameRepository;
import server.game.ProposalService;
import shared.dto.GameInfoData;

import java.util.HashMap;
import java.util.Map;

public final class GameTransitionWatcherImpl implements GameTransitionWatcher {
    private final GameClock gameClock;
    private final PlayerGameRepository playerGameRepository;
    private final ProposalService proposalService;
    private final NotificationService notificationService;
    private final long pollIntervalMillis;
    private volatile boolean shouldStop = false;
    private long lastObservedGameId = -1;

    public GameTransitionWatcherImpl(GameClock gameClock,
                                     PlayerGameRepository playerGameRepository,
                                     ProposalService proposalService,
                                     NotificationService notificationService,
                                     long pollIntervalMillis) {
        this.gameClock = gameClock;
        this.playerGameRepository = playerGameRepository;
        this.proposalService = proposalService;
        this.notificationService = notificationService;
        this.pollIntervalMillis = pollIntervalMillis;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        long currentId = gameClock.currentGameId(now);
        lastObservedGameId = currentId;
        while (!shouldStop && !Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(pollIntervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            now = System.currentTimeMillis();
            currentId = gameClock.currentGameId(now);
            if (currentId != lastObservedGameId) {
                handleGameTransition(lastObservedGameId);
                lastObservedGameId = currentId;
            }
        }
    }

    public void stop() {
        shouldStop = true;
    }

    private void handleGameTransition(long endedGameId) {
        Map<String, GameInfoData> results = new HashMap<>();
        for (PlayerGame pg : playerGameRepository.findByGame(endedGameId)) {
            try {
                GameInfoData info = proposalService.getGameInfoForUsername(endedGameId, pg.username());
                results.put(pg.username(), info);
            } catch (Exception e) {
                // Log if needed
            }
        }
        notificationService.notifyGameEnd(results);
    }
}