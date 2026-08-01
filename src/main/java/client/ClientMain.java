package client;

import shared.JsonCodec;
import shared.Request;
import shared.Response;

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

    public Response send_message(Request request) throws IOException {
        String payload = JsonCodec.serialize(request) + "\n";
        
        socketChannel.write(ByteBuffer.wrap(payload.getBytes(StandardCharsets.UTF_8)));

        buffer.clear();
        int bytesRead = socketChannel.read(buffer);
        if (bytesRead == -1) {
            throw new IOException("Connection closed by the server.");
        }

        buffer.flip();
        String rawResponse = StandardCharsets.UTF_8.decode(buffer).toString().trim();

        return JsonCodec.deserialize(rawResponse, Response.class);
    }

    @Override
    public void close() throws IOException {
        if (socketChannel != null) {
            socketChannel.close();
        }
    }

    public static void main(String[] args) {
        try (ClientMain client = ClientMain.create("localhost", 8080)) {
            
            System.out.println(client.send_message(new Request("uppercase", "functional pipelines rule")));
            System.out.println(client.send_message(new Request("reverse", "no static configurations")));

        } catch (IOException e) {
            System.err.println("Client runtime error: " + e.getMessage());
        }
    }
}