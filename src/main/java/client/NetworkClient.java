package client;

import shared.JsonCodec;
import shared.Request;
import shared.Response;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public final class NetworkClient implements AutoCloseable {
    private final SocketChannel socketChannel;
    private final BufferedReader reader;
    private final PrintWriter writer;

    public NetworkClient(String host, int port) throws IOException {
        this.socketChannel = SocketChannel.open(new InetSocketAddress(host, port));
        this.reader = new BufferedReader(Channels.newReader(socketChannel, StandardCharsets.UTF_8));
        this.writer = new PrintWriter(Channels.newWriter(socketChannel, StandardCharsets.UTF_8), true);
    }

    public <T> Response<T> sendRequest(Request request, Class<T> responseType) throws IOException {
        String payload = JsonCodec.serialize(request);
        writer.println(payload);
        String line = reader.readLine();
        if (line == null) {
            throw new IOException("Connection closed by server.");
        }
        return JsonCodec.deserializeResponse(line.trim(), responseType);
    }

    @Override
    public void close() throws IOException {
        if (socketChannel != null && socketChannel.isOpen()) {
            socketChannel.close();
        }
    }
}