package server;

import shared.JsonCodec;
import shared.Request;
import shared.Response;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class ServerMain {
    private static final int UDP_PORT = 9876;
    private final UserManager userManager;
    private final GameManager gameManager;
    private final RequestProcessor requestProcessor;
    private final AtomicLong lastNotifiedGameId = new AtomicLong(-1);

    public ServerMain() {
        this.userManager = new UserManager();
        this.gameManager = new GameManager("Connections_Data.json", Duration.ofMinutes(10), 5);
        this.requestProcessor = new RequestProcessor(gameManager, userManager);
    }

    public Response dispatch(Request request, String currentUser) {
        return requestProcessor.handle(request, currentUser);
    }

    public void start(int port) {
        startUdpNotifier();
        Thread.ofPlatform().start(() -> {
            try (var server = new ServerSocket(port);
                 var executor = Executors.newFixedThreadPool(10)) {
                System.out.println("Server listening on port " + port);
                while (!Thread.currentThread().isInterrupted()) {
                    Socket socket = server.accept();
                    executor.submit(() -> handleClient(socket));
                }
            } catch (Exception e) {
                System.err.println("Server error: " + e.getMessage());
            }
        });
    }

    private void startUdpNotifier() {
        Thread.ofPlatform().daemon().start(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                while (!Thread.currentThread().isInterrupted()) {
                    long currentGameId = gameManager.getCurrentGameId();
                    var activeGame = gameManager.getActiveGame();
                    if (gameManager.getRemainingTime(activeGame).isZero() && lastNotifiedGameId.get() != currentGameId + 1) {
                        long nextGameId = currentGameId + 1;
                        lastNotifiedGameId.set(nextGameId);
                        byte[] data = ("GAME_UPDATE:" + nextGameId).getBytes(StandardCharsets.UTF_8);
                        DatagramPacket packet = new DatagramPacket(
                                data,
                                data.length,
                                InetAddress.getByName("255.255.255.255"),
                                UDP_PORT
                        );
                        socket.send(packet);
                    }
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                System.err.println("UDP Notifier error: " + e.getMessage());
            }
        });
    }

    private void handleClient(Socket socket) {
        var currentUser = new AtomicReference<String>(null);
        try (socket;
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             var writer = new PrintWriter(socket.getOutputStream(), true)) {
            reader.lines()
                    .filter(line -> !line.isBlank())
                    .map(line -> processRequest(line, currentUser))
                    .forEach(writer::println);
        } catch (Exception e) {
            System.err.println("Client session closed: " + e.getMessage());
        }
    }

    private String processRequest(String jsonLine, AtomicReference<String> currentUser) {
        try {
            Request request = JsonCodec.deserialize(jsonLine, Request.class);
            Response response = dispatch(request, currentUser.get());
            if (response.success()) {
                updateSessionState(request, currentUser);
            }
            return JsonCodec.serialize(response);
        } catch (Exception e) {
            return JsonCodec.serializeError(e.getMessage());
        }
    }

    private void updateSessionState(Request request, AtomicReference<String> currentUser) {
        if (request instanceof Request.Login login) {
            currentUser.set(login.username());
        } else if (request instanceof Request.Logout) {
            currentUser.set(null);
        } else if (request instanceof Request.UpdateCredentials update
                && update.newUsername() != null
                && !update.newUsername().isBlank()) {
            currentUser.set(update.newUsername());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ServerMain server = new ServerMain();
        server.start(8080);
        Thread.currentThread().join();
    }
}