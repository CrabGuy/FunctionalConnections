package server.game;

/**
 * Implementation of {@link GameClock} that divides time into fixed-length games.
 *
 * @param gameDurationMillis the duration of each game in milliseconds
 */
public record GameClockImpl(long gameDurationMillis) implements GameClock {

  /** {@inheritDoc} */
  @Override
  public long currentGameId(long nowMillis) {
    return nowMillis / gameDurationMillis;
  }

  /** {@inheritDoc} */
  @Override
  public long startedAt(long gameId) {
    return gameId * gameDurationMillis;
  }

  /** {@inheritDoc} */
  @Override
  public long expiresAt(long gameId) {
    return startedAt(gameId) + gameDurationMillis;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isCompleted(long gameId, long nowMillis) {
    return nowMillis >= expiresAt(gameId);
  }
}
