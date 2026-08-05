package server;

import shared.JsonCodec;
import shared.Request;
import shared.Response;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

public class ServerMain {

    public Response dispatch(Request request) {
        if (request == null || request.operation() == null) {
            return new Response(false, null, "Invalid request format");
        }
        return request.handle();
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
        try (socket;
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             var writer = new PrintWriter(socket.getOutputStream(), true)) {

            reader.lines()
                  .filter(line -> !line.isBlank())
                  .map(this::parseAndExecute)
                  .forEach(writer::println);

        } catch (Exception e) {
            System.err.println("Client session closed: " + e.getMessage());
        }
    }

    private String parseAndExecute(String jsonLine) {
        try {
            Request request = JsonCodec.deserialize(jsonLine, Request.class);
            Response response = dispatch(request);
            return JsonCodec.serialize(response);
        } catch (Exception e) {
            e.printStackTrace(); // Prints exact missing Jackson field/type
            return JsonCodec.serializeError(e.getMessage());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ServerMain server = new ServerMain();
        server.start(8080);

        Thread.currentThread().join();
    }
}