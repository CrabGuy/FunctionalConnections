package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class ClientMain implements AutoCloseable {
    private final SocketChannel socketChannel;
    private final ByteBuffer buffer = ByteBuffer.allocate(1024);

    private ClientMain(String host, int port) throws IOException {
        this.socketChannel = SocketChannel.open(new InetSocketAddress(host, port));
    }

    public static ClientMain create(String host, int port) throws IOException {
        return new ClientMain(host, port);
    }

    public String send_message(String message) throws IOException {
        String payload = message + "\n";
        ByteBuffer writeBuffer = ByteBuffer.wrap(payload.getBytes(StandardCharsets.UTF_8));
        
        while (writeBuffer.hasRemaining()) {
            socketChannel.write(writeBuffer);
        }

        buffer.clear();
        int bytesRead = socketChannel.read(buffer);
        
        if (bytesRead == -1) {
            throw new IOException("Connection closed by the server.");
        }

        buffer.flip();
        String response = StandardCharsets.UTF_8.decode(buffer).toString();
        
        return response.trim();
    }

    @Override
    public void close() throws IOException {
        if (socketChannel != null) {
            socketChannel.close();
        }
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