package server.network;

/**
 * Background thread that periodically compares GameClock.currentGameId(now) to the last observed id.
 * On change, gathers active participants and calls NotificationService.notifyGameEnd.
 * Owns no game state itself — purely a clock-driven trigger.
 */
public interface GameTransitionWatcher extends Runnable {
    
    /**
     * Initiates the background monitoring process.
     */
    void startWatching();
    
    /**
     * Stops the background monitoring process gracefully.
     */
    void stopWatching();
}