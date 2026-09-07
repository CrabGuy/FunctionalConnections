package client.notification;

import client.json.JsonCodec;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;
import shared.dto.GameEndNotification;

public final class UdpNotificationListener implements NotificationListener {
    private volatile boolean running;
    private DatagramSocket socket;
    private Thread thread;

    @Override
    public void start(int udpPort, Consumer<GameEndNotification> onGameEnd) throws IOException {
        Objects.requireNonNull(onGameEnd, "onGameEnd");
        stop();
        socket = new DatagramSocket(udpPort);
        running = true;
        thread = new Thread(() -> listen(onGameEnd), "client-udp-notifications");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void stop() {
        running = false;
        if (socket != null) {
            socket.close();
            socket = null;
        }
        if (thread != null && thread != Thread.currentThread()) {
            thread.interrupt();
            thread = null;
        }
    }

    private void listen(Consumer<GameEndNotification> onGameEnd) {
        byte[] buffer = new byte[65_507];
        try {
            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String json =
                        new String(
                                packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
                GameEndNotification notification = JsonCodec.gameEndNotificationFromJson(json);
                try {
                    onGameEnd.accept(notification);
                } catch (RuntimeException e) {
                    System.err.println("Notification callback error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Notification listener error: " + e.getMessage());
            }
        }
    }
}