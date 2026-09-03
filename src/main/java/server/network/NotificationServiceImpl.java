package server.network;

import com.google.gson.Gson;
import server.account.NotificationRegistry;
import shared.dto.GameInfoData;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class NotificationServiceImpl implements NotificationService {
    private final NotificationRegistry registry;
    private final Gson gson;

    public NotificationServiceImpl(NotificationRegistry registry, Gson gson) {
        this.registry = registry;
        this.gson = gson;
    }

    @Override
    public void notifyGameEnd(Map<String, GameInfoData> resultsByUsername) {
        try (DatagramSocket socket = new DatagramSocket()) {
            for (Map.Entry<String, GameInfoData> entry : resultsByUsername.entrySet()) {
                registry.lookup(entry.getKey()).ifPresent(address -> {
                    try {
                        byte[] payload = gson.toJson(entry.getValue()).getBytes(StandardCharsets.UTF_8);
                        DatagramPacket packet = new DatagramPacket(payload, payload.length, address);
                        socket.send(packet);
                    } catch (Exception e) {
                        // log and continue
                    }
                });
            }
        } catch (Exception e) {
            // log
        }
    }
}