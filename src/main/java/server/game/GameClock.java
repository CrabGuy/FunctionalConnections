package server.game;

import java.time.Instant;

public interface GameClock {
    long currentGameId();
    Instant startTimeForGameId(long gameId);
    Instant now();
}