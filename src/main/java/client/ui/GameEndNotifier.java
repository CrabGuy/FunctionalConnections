package client.ui;

import java.util.concurrent.atomic.AtomicBoolean;

public final class GameEndNotifier {
    private final AtomicBoolean pending = new AtomicBoolean(false);
    private volatile long gameId;

    public void signal(long gameId) {
        this.gameId = gameId;
        pending.set(true);
    }

    public boolean consumeIfPending() {
        return pending.getAndSet(false);
    }

    public long getGameId() {
        return gameId;
    }
}