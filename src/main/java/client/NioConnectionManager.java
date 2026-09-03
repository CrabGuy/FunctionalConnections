package client;

import client.json.ProtocolCodec;
import shared.dto.ApiRequest;
import shared.dto.ApiResponse;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public record NioConnectionManager(PersistentNioConnection connection) implements ConnectionManager {

    public NioConnectionManager() {
        this(new PersistentNioConnection());
    }

    @Override
    public void connect(String host, int port) throws IOException {
        connection.connect(host, port);
    }

    @Override
    public ApiResponse<?> send(ApiRequest request) throws IOException {
        String response = connection.send(ProtocolCodec.requestToJson(request));
        return ProtocolCodec.responseFromJson(response, request.getOperation());
    }

    @Override
    public void close() throws IOException {
        connection.close();
    }

    public static final class PersistentNioConnection {
        private SocketChannel channel;
        private BufferedReader reader;
        private BufferedWriter writer;

        public void connect(String host, int port) throws IOException {
            close();
            SocketChannel newChannel = SocketChannel.open();
            newChannel.configureBlocking(true);
            try {
                newChannel.connect(new InetSocketAddress(host, port));
            } catch (IOException e) {
                newChannel.close();
                throw e;
            }
            channel = newChannel;
            reader = new BufferedReader(Channels.newReader(newChannel, StandardCharsets.UTF_8));
            writer = new BufferedWriter(Channels.newWriter(newChannel, StandardCharsets.UTF_8));
        }

        public String send(String json) throws IOException {
            ensureConnected();
            writer.write(json);
            writer.newLine();
            writer.flush();
            return reader.readLine();
        }

        public void close() throws IOException {
            if (channel != null) {
                try {
                    channel.close();
                } finally {
                    channel = null;
                    reader = null;
                    writer = null;
                }
            }
        }

        private void ensureConnected() throws IOException {
            if (channel == null || !channel.isOpen()) {
                throw new IOException("Client is not connected to the server");
            }
        }
    }
}