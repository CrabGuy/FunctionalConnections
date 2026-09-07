package client.ui;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A thread-safe flag that signals when a game end notification has been received, storing the game
 * ID for later retrieval.
 */
public final class GameEndNotifier {

  private final AtomicBoolean pending = new AtomicBoolean(false);
  private volatile long gameId;

  /**
   * Sets the pending flag and stores the game ID.
   *
   * @param gameId the ID of the ended game
   */
  public void signal(long gameId) {
    this.gameId = gameId;
    pending.set(true);
  }

  /**
   * Checks and clears the pending flag.
   *
   * @return true if a notification was pending, false otherwise
   */
  public boolean consumeIfPending() {
    return pending.getAndSet(false);
  }

  /**
   * Returns the stored game ID.
   *
   * @return the game ID, or 0 if not set
   */
  public long getGameId() {
    return gameId;
  }
}
