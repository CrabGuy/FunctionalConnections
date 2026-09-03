package server.game;

/**
 * Pure functional implementation of GameClock.
 * Game IDs are derived from elapsed time, starting at 0.
 */
public record GameClockImpl(long gameDurationMillis) implements GameClock {

    @Override
    public long currentGameId(long nowMillis) {
        return nowMillis / gameDurationMillis;
    }

    @Override
    public long startedAt(long gameId) {
        return gameId * gameDurationMillis;
    }

    @Override
    public long expiresAt(long gameId) {
        return startedAt(gameId) + gameDurationMillis;
    }

    @Override
    public boolean isCompleted(long gameId, long nowMillis) {
        return nowMillis >= expiresAt(gameId);
    }
}