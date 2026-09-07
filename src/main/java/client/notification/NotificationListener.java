package client.notification;

import java.io.IOException;
import java.util.function.Consumer;
import shared.dto.GameEndNotification;

/**
 * Defines the contract for listening to UDP notifications from the server, specifically game end
 * events.
 */
public interface NotificationListener {

  /**
   * Starts listening on the given UDP port and invokes the callback when a game end notification is
   * received.
   *
   * @param udpPort the local UDP port to bind
   * @param onGameEnd callback to handle received notifications
   * @throws IOException if the socket cannot be opened
   */
  void start(int udpPort, Consumer<GameEndNotification> onGameEnd) throws IOException;

  /** Stops listening and releases resources. */
  void stop();
}
