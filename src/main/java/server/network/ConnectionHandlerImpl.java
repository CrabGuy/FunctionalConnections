package server.network;

import com.google.gson.Gson;
import shared.dto.ApiRequest;
import shared.dto.ApiResponse;
import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;

public final class ConnectionHandlerImpl implements ConnectionHandler {
    private final Gson gson;
    private final RequestDispatcher dispatcher;
    private SocketChannel clientChannel;

    public ConnectionHandlerImpl(RequestDispatcher dispatcher, Gson gson) {
        this.dispatcher = dispatcher;
        this.gson = gson;
    }

    @Override
    public void bind(SocketChannel clientChannel) {
        this.clientChannel = clientChannel;
    }

    @Override
    public void run() {
        if (clientChannel == null) {
            throw new IllegalStateException("bind() must be called before run()");
        }
        try (BufferedReader reader = new BufferedReader(Channels.newReader(clientChannel, "UTF-8"));
             PrintWriter writer = new PrintWriter(Channels.newWriter(clientChannel, "UTF-8"), true)) {

            String line;
            while ((line = reader.readLine()) != null) {
                ApiRequest request = gson.fromJson(line, ApiRequest.class);
                ApiResponse<?> response = dispatcher.dispatch(request);
                writer.println(gson.toJson(response));
            }
        } catch (IOException e) {
            // Log or ignore connection errors
        } finally {
            try {
                clientChannel.close();
            } catch (IOException ignored) {}
        }
    }
}