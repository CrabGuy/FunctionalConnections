package server.network;

import server.game.GameClock;

/**
 * Implementation of {@link GameTransitionWatcher} that polls the game clock and sends notifications
 * when a new game starts.
 */
public final class GameTransitionWatcherImpl implements GameTransitionWatcher {

  private final GameClock gameClock;
  private final NotificationService notificationService;
  private final long pollIntervalMillis;
  private volatile boolean shouldStop = false;
  private long lastObservedGameId = -1;

  /**
   * Constructs the watcher.
   *
   * @param gameClock the game clock
   * @param notificationService the notification service
   * @param pollIntervalMillis the polling interval in milliseconds
   */
  public GameTransitionWatcherImpl(
      GameClock gameClock, NotificationService notificationService, long pollIntervalMillis) {
    this.gameClock = gameClock;
    this.notificationService = notificationService;
    this.pollIntervalMillis = pollIntervalMillis;
  }

  /** Runs the watcher loop until stopped. */
  @Override
  public void run() {
    long now = System.currentTimeMillis();
    lastObservedGameId = gameClock.currentGameId(now);
    while (!shouldStop && !Thread.currentThread().isInterrupted()) {
      try {
        Thread.sleep(pollIntervalMillis);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
      long currentId = gameClock.currentGameId(System.currentTimeMillis());
      if (currentId != lastObservedGameId) {
        notificationService.notifyGameEnd(lastObservedGameId);
        lastObservedGameId = currentId;
      }
    }
  }

  /** Stops the watcher. */
  public void stop() {
    shouldStop = true;
  }
}
