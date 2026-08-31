package client;

import config.ClientConfig;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public final class ClientMain {
    public static void main(String[] args) {
        try {
            ClientConfig config = ClientConfig.load();
            NetworkSession session = new NetworkSession(config.serverHost(), config.serverPort());
            ConsoleView view = new ConsoleView();
            GameObserver observer = new GameObserver();
            startUdpListener(config.udpPort(), observer);
            GameLoopController controller = new GameLoopController(session, view, observer);
            try (Scanner scanner = new Scanner(System.in)) {
                controller.run(scanner);
            }
        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    private static void startUdpListener(int udpPort, GameObserver observer) {
        Thread.ofPlatform().daemon().start(() -> {
            try (DatagramSocket socket = new DatagramSocket(udpPort)) {
                byte[] buffer = new byte[1024];
                while (!Thread.currentThread().isInterrupted()) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();
                    observer.onUdpMessage(message);
                }
            } catch (Exception e) {
                System.err.println("UDP Listener error: " + e.getMessage());
            }
        });
    }
}