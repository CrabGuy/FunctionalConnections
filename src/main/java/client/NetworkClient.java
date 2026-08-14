package client;

import shared.JsonCodec;
import shared.Request;
import shared.Response;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public final class NetworkClient implements AutoCloseable {
    private final SocketChannel socketChannel;

    public NetworkClient(String host, int port) throws IOException {
        this.socketChannel = SocketChannel.open(new InetSocketAddress(host, port));
    }

    public Response sendRequest(Request request) throws IOException {
        String payload = JsonCodec.serialize(request) + "\n";
        socketChannel.write(ByteBuffer.wrap(payload.getBytes(StandardCharsets.UTF_8)));

        ByteBuffer buffer = ByteBuffer.allocate(4096);
        int bytesRead = socketChannel.read(buffer);
        if (bytesRead == -1) {
            throw new IOException("Connection closed by server.");
        }

        buffer.flip();
        String rawResponse = StandardCharsets.UTF_8.decode(buffer).toString().trim();
        return JsonCodec.deserialize(rawResponse, Response.class);
    }

    @Override
    public void close() throws IOException {
        if (socketChannel != null && socketChannel.isOpen()) {
            socketChannel.close();
        }
    }
}