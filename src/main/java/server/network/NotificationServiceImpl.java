package server.network;

import com.google.gson.Gson;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import server.account.NotificationRegistry;
import shared.dto.GameEndNotification;

/**
 * Implementation of {@link NotificationService} that sends UDP packets to all registered client
 * addresses.
 */
public final class NotificationServiceImpl implements NotificationService, AutoCloseable {

  private final DatagramSocket socket;
  private final NotificationRegistry registry;
  private final Gson gson;

  /**
   * Constructs the service with a new UDP socket.
   *
   * @param registry the notification registry
   * @param gson the Gson instance
   * @throws SocketException if the socket cannot be created
   */
  public NotificationServiceImpl(NotificationRegistry registry, Gson gson) throws SocketException {
    this.registry = registry;
    this.gson = gson;
    this.socket = new DatagramSocket();
  }

  /** {@inheritDoc} */
  @Override
  public void notifyGameEnd(long gameId) {
    Set<String> usernames = registry.getRegisteredUsernames();
    byte[] payload = gson.toJson(new GameEndNotification(gameId)).getBytes(StandardCharsets.UTF_8);
    for (String username : usernames) {
      registry
          .lookup(username)
          .ifPresent(
              address -> {
                try {
                  DatagramPacket packet = new DatagramPacket(payload, payload.length, address);
                  socket.send(packet);
                } catch (Exception ignored) {
                }
              });
    }
  }

  /** Closes the UDP socket. */
  @Override
  public void close() {
    socket.close();
  }
}
