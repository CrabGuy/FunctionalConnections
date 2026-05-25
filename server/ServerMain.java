package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class ServerMain {

    // Private constructor to enforce the use of the factory method
    private ServerMain(int port_number, Function<String, String> transformer_logic) {
        Thread.ofPlatform().start(() -> run_server(port_number, transformer_logic));
    }

    // Static factory method
    public static ServerMain create(int port_number, Function<String, String> transformer_logic) {
        return new ServerMain(port_number, transformer_logic);
    }

    private void run_server(int port, Function<String, String> transformer) {
        try (var server_socket = new ServerSocket(port);
             var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            System.out.println("Server automatically started on port " + port);

            while (!Thread.currentThread().isInterrupted()) {
                Socket client_socket = server_socket.accept();
                executor.submit(() -> handle_client(client_socket, transformer));
            }
        } catch (IOException e) {
            System.err.println("Server execution error: " + e.getMessage());
        }
    }

    private void handle_client(Socket socket, Function<String, String> transformer) {
        try (socket;
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             var writer = new PrintWriter(socket.getOutputStream(), true)) {

            reader.lines()
                  .map(transformer)
                  .forEach(writer::println);

        } catch (IOException e) {
            System.err.println("Client session error: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // Pass everything purely as functional parameters
        ServerMain.create(8080, String::toUpperCase);
    }
}