package client;

import client.json.ProtocolCodec;
import shared.dto.GameInfoData;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;

/** Receives asynchronous game-end notifications on a dedicated thread. */
public record UdpNotificationListener(NotificationRuntime runtime) implements NotificationListener {
    public UdpNotificationListener() {
        this(new NotificationRuntime());
    }

    @Override
    public void start(int udpPort, Consumer<GameInfoData> onGameEnd) throws IOException {
        runtime.start(udpPort, Objects.requireNonNull(onGameEnd, "onGameEnd"));
    }

    @Override
    public void stop() {
        runtime.stop();
    }

    /** Stateful UDP boundary kept behind the injectable listener implementation. */
    public static final class NotificationRuntime {
        private final Object lock = new Object();
        private volatile boolean running;
        private DatagramSocket socket;
        private Thread thread;

        public void start(int udpPort, Consumer<GameInfoData> onGameEnd) throws IOException {
            synchronized (lock) {
                stopInternal();
                DatagramSocket newSocket = new DatagramSocket(udpPort);
                socket = newSocket;
                running = true;
                thread = new Thread(
                        () -> listen(newSocket, onGameEnd),
                        "client-udp-notifications");
                thread.setDaemon(true);
                thread.start();
            }
        }

        public void stop() {
            synchronized (lock) {
                stopInternal();
            }
        }

        private void listen(DatagramSocket listeningSocket, Consumer<GameInfoData> onGameEnd) {
            byte[] buffer = new byte[65_507];
            try {
                while (running) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    listeningSocket.receive(packet);
                    String json = new String(
                            packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
                    GameInfoData info = ProtocolCodec.gameInfoFromJson(json);
                    try {
                        onGameEnd.accept(info);
                    } catch (RuntimeException exception) {
                        System.err.println("Notification callback error: " + exception.getMessage());
                    }
                }
            } catch (IOException | RuntimeException exception) {
                if (running) {
                    System.err.println("Notification listener error: " + exception.getMessage());
                }
            }
        }

        private void stopInternal() {
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
    }
}
