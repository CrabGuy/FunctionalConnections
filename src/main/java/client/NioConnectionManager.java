package client;

import client.json.ProtocolCodec;
import shared.dto.ApiRequest;
import shared.dto.ApiResponse;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

/** Persistent newline-delimited TCP connection using Java NIO. */
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

    /** Stateful I/O boundary kept outside the record fields used for dependency injection. */
    public static final class PersistentNioConnection {
        private SocketChannel channel;
        private byte[] pending = new byte[0];

        public void connect(String host, int port) throws IOException {
            close();
            SocketChannel newChannel = SocketChannel.open();
            newChannel.configureBlocking(true);
            try {
                newChannel.connect(new InetSocketAddress(host, port));
            } catch (IOException exception) {
                newChannel.close();
                throw exception;
            }
            channel = newChannel;
            pending = new byte[0];
        }

        public String send(String json) throws IOException {
            ensureConnected();
            writeFully((json + "\n").getBytes(StandardCharsets.UTF_8));
            return readLine();
        }

        public void close() throws IOException {
            if (channel == null) {
                return;
            }
            try {
                channel.close();
            } finally {
                channel = null;
                pending = new byte[0];
            }
        }

        private void writeFully(byte[] bytes) throws IOException {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        }

        private String readLine() throws IOException {
            while (true) {
                int newline = indexOfNewline(pending);
                if (newline >= 0) {
                    return takePendingLine(newline);
                }

                ByteBuffer buffer = ByteBuffer.allocate(4096);
                int read = channel.read(buffer);
                if (read < 0) {
                    throw new EOFException("Server closed the TCP connection before sending a response");
                }
                if (read > 0) {
                    appendPending(buffer, read);
                }
            }
        }

        private String takePendingLine(int newline) {
            int contentLength = newline;
            if (contentLength > 0 && pending[contentLength - 1] == '\r') {
                contentLength--;
            }

            String line = new String(pending, 0, contentLength, StandardCharsets.UTF_8);
            int remainingLength = pending.length - newline - 1;
            byte[] remaining = new byte[remainingLength];
            System.arraycopy(pending, newline + 1, remaining, 0, remainingLength);
            pending = remaining;
            return line;
        }

        private void appendPending(ByteBuffer buffer, int length) {
            buffer.flip();
            byte[] chunk = new byte[length];
            buffer.get(chunk);
            byte[] merged = new byte[pending.length + chunk.length];
            System.arraycopy(pending, 0, merged, 0, pending.length);
            System.arraycopy(chunk, 0, merged, pending.length, chunk.length);
            pending = merged;
        }

        private static int indexOfNewline(byte[] bytes) {
            for (int i = 0; i < bytes.length; i++) {
                if (bytes[i] == '\n') {
                    return i;
                }
            }
            return -1;
        }

        private void ensureConnected() throws IOException {
            if (channel == null || !channel.isOpen()) {
                throw new IOException("Client is not connected to the server");
            }
        }
    }
}
