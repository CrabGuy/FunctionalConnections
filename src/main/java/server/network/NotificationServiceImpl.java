package server.network;

import com.google.gson.Gson;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import server.account.NotificationRegistry;
import shared.dto.GameEndNotification;

public final class NotificationServiceImpl implements NotificationService, AutoCloseable {
  private final DatagramSocket socket;
  private final NotificationRegistry registry;
  private final Gson gson;

  public NotificationServiceImpl(NotificationRegistry registry, Gson gson) throws SocketException {
    this.registry = registry;
    this.gson = gson;
    this.socket = new DatagramSocket();
  }

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

  @Override
  public void close() {
    socket.close();
  }
}
