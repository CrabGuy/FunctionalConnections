package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientMain implements AutoCloseable {
    private final Socket socket;
    private final PrintWriter writer;
    private final BufferedReader reader;

    private ClientMain(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.writer = new PrintWriter(socket.getOutputStream(), true);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    // Static factory method
    public static ClientMain create(String host, int port) throws IOException {
        return new ClientMain(host, port);
    }

    public String send_message(String message) throws IOException {
        writer.println(message);
        return reader.readLine();
    }

    @Override
    public void close() throws IOException {
        reader.close();
        writer.close();
        socket.close();
    }

    public static void main(String[] args) {
        try (ClientMain client = ClientMain.create("localhost", 8080)) {
            
            System.out.println(client.send_message("functional pipelines rule"));
            System.out.println(client.send_message("no static configurations"));

        } catch (IOException e) {
            System.err.println("Client runtime error: " + e.getMessage());
        }
    }
}