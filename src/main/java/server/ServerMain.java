package server;

import shared.JsonCodec;
import shared.Request;
import shared.Response;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class ServerMain {

    private final GameManager gameManager;
    private final UserManager userManager;

    public ServerMain() {
        this.userManager = new UserManager();
        this.gameManager = new GameManager("Connections_Data.json", Duration.ofHours(1), 5);
    }

    public Response dispatch(Request request, String currentUser) {
        return Optional.ofNullable(request)
            .filter(r -> r.operation() != null)
            .map(r -> r.handle(gameManager, userManager, currentUser))
            .orElseGet(() -> new Response(false, null, "Invalid request format"));
    }

    public void start(int port) {
        Thread.ofPlatform().start(() -> {
            try (var server = new ServerSocket(port);
                 var executor = Executors.newVirtualThreadPerTaskExecutor()) {

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
        } else if (request instanceof Request.UpdateCredentials update && update.newUsername() != null && !update.newUsername().isBlank()) {
            currentUser.set(update.newUsername());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ServerMain server = new ServerMain();
        server.start(8080);

        Thread.currentThread().join();
    }
}