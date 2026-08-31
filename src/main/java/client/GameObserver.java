package client;

import java.util.concurrent.atomic.AtomicLong;

public final class GameObserver {
    private final AtomicLong lastSeenGameId = new AtomicLong(-1);
    private volatile long currentGameId = -1;

    public void onUdpMessage(String message) {
        if (message.startsWith("GAME_UPDATE:")) {
            try {
                long newId = Long.parseLong(message.substring("GAME_UPDATE:".length()).trim());
                currentGameId = newId;
            } catch (NumberFormatException e) {
                // ignore
            }
        }
    }

    public boolean hasNewGame() {
        long current = currentGameId;
        long lastSeen = lastSeenGameId.get();
        if (current != lastSeen) {
            lastSeenGameId.set(current);
            return true;
        }
        return false;
    }

    public long currentGameId() {
        return currentGameId;
    }
}