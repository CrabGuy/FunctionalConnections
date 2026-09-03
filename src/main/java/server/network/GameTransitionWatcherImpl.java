package server.network;

import server.dto.PlayerGame;
import server.game.GameClock;
import server.game.PlayerGameRepository;
import server.game.ProposalService;
import shared.dto.GameInfoData;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GameTransitionWatcherImpl implements GameTransitionWatcher {
    private final GameClock gameClock;
    private final PlayerGameRepository playerGameRepository;
    private final ProposalService proposalService;
    private final NotificationService notificationService;
    private final long pollIntervalMillis;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread watcherThread;
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
    public void startWatching() {
        if (running.compareAndSet(false, true)) {
            watcherThread = new Thread(this, "game-transition-watcher");
            watcherThread.setDaemon(true);
            watcherThread.start();
        }
    }

    @Override
    public void stopWatching() {
        running.set(false);
        if (watcherThread != null) {
            watcherThread.interrupt();
        }
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        long currentId = gameClock.currentGameId(now);
        lastObservedGameId = currentId; // don't notify for the game already active at startup
        while (running.get()) {
            try {
                Thread.sleep(pollIntervalMillis);
            } catch (InterruptedException e) {
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

    private void handleGameTransition(long endedGameId) {
        Map<String, GameInfoData> results = new HashMap<>();
        for (PlayerGame pg : playerGameRepository.findByGame(endedGameId)) {
            try {
                GameInfoData info = proposalService.getGameInfoForUsername(endedGameId, pg.username());
                results.put(pg.username(), info);
            } catch (Exception e) {
                // skip players for whom info cannot be retrieved
            }
        }
        notificationService.notifyGameEnd(results);
    }
}