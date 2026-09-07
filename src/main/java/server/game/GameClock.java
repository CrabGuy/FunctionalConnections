package server.game;

/** Provides time-based game identification and expiry calculations. */
public interface GameClock {

  /**
   * Returns the current game ID based on the given time.
   *
   * @param nowMillis the current time in milliseconds
   * @return the game ID
   */
  long currentGameId(long nowMillis);

  /**
   * Returns the start time of the given game.
   *
   * @param gameId the game ID
   * @return the start time in milliseconds
   */
  long startedAt(long gameId);

  /**
   * Returns the expiry time of the given game.
   *
   * @param gameId the game ID
   * @return the expiry time in milliseconds
   */
  long expiresAt(long gameId);

  /**
   * Checks if the given game has already completed based on the current time.
   *
   * @param gameId the game ID
   * @param nowMillis the current time in milliseconds
   * @return true if completed, false otherwise
   */
  boolean isCompleted(long gameId, long nowMillis);
}
