package server.network;

import com.google.gson.Gson;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import server.account.NotificationRegistry;
import shared.dto.GameInfoData;

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
  public void notifyGameEnd(Map<String, GameInfoData> resultsByUsername) {
    for (Map.Entry<String, GameInfoData> entry : resultsByUsername.entrySet()) {
      registry
          .lookup(entry.getKey())
          .ifPresent(
              address -> {
                try {
                  byte[] payload = gson.toJson(entry.getValue()).getBytes(StandardCharsets.UTF_8);
                  DatagramPacket packet = new DatagramPacket(payload, payload.length, address);
                  socket.send(packet);
                } catch (Exception e) {
                  // log and continue
                }
              });
    }
  }

  @Override
  public void close() {
    socket.close();
  }
}
