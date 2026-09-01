package server.game;

import java.time.Duration;
import java.time.Instant;

public final class SystemGameClock implements GameClock {
    private final Duration gameDuration;

    public SystemGameClock(Duration gameDuration) {
        this.gameDuration = gameDuration;
    }

    @Override
    public long currentGameId() {
        return Instant.now().toEpochMilli() / gameDuration.toMillis();
    }

    @Override
    public Instant startTimeForGameId(long gameId) {
        return Instant.ofEpochMilli(gameId * gameDuration.toMillis());
    }

    @Override
    public Instant now() {
        return Instant.now();
    }
}