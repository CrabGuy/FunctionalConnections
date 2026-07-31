package server;

import shared.MessageCodec;
import shared.Request;
import shared.Response;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class ServerMain {

    public static void start(int port, Function<Request, Response> handler) {
        Thread.ofPlatform().start(() -> {
            try (var server = new ServerSocket(port);
                 var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                
                System.out.println("Server listening on port " + port);
                while (!Thread.currentThread().isInterrupted()) {
                    Socket socket = server.accept();
                    executor.submit(() -> handleClient(socket, handler));
                }
            } catch (Exception e) {
                System.err.println("Server error: " + e.getMessage());
            }
        });
    }

    private static void handleClient(Socket socket, Function<Request, Response> handler) {
        try (socket;
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             var writer = new PrintWriter(socket.getOutputStream(), true)) {

            reader.lines()
                  .filter(line -> !line.isBlank())
                  .map(line -> parseAndExecute(line, handler))
                  .forEach(writer::println);

        } catch (Exception e) {
            System.err.println("Client session closed: " + e.getMessage());
        }
    }

    private static String parseAndExecute(String jsonLine, Function<Request, Response> handler) {
        try {
            Request request = MessageCodec.deserialize(jsonLine, Request.class);
            Response response = handler.apply(request);
            return MessageCodec.serialize(response);
        } catch (Exception e) {
            return MessageCodec.serializeError(e.getMessage());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Function<Request, Response> logic = req -> switch (req.command().toLowerCase()) {
            case "uppercase" -> new Response(true, req.payload().toUpperCase(), null);
            case "reverse"   -> new Response(true, new StringBuilder(req.payload()).reverse().toString(), null);
            default          -> new Response(false, null, "Unknown command");
        };

        ServerMain.start(8080, logic);

        Thread.currentThread().join();
    }
}