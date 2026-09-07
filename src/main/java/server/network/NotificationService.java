package server.network;

/** Service for sending game end notifications to clients. */
public interface NotificationService {

  /**
   * Sends a game end notification for the given game ID to all registered clients.
   *
   * @param gameId the game ID that ended
   */
  void notifyGameEnd(long gameId);
}
