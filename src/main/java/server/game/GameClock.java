package server.game;
public interface GameClock {
    long currentGameId(long nowMillis);
    long startedAt(long gameId);
    long expiresAt(long gameId);
    boolean isCompleted(long gameId, long nowMillis);
}
