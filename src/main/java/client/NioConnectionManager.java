package client;

import shared.dto.ApiRequest;
import shared.dto.ApiResponse;

import java.io.IOException;

public record NioConnectionManager(TcpConnection connection) implements ConnectionManager {
    public NioConnectionManager() {
        this(new TcpConnection());
    }

    @Override
    public void connect(String host, int port) throws IOException {
        connection.connect(host, port);
    }

    @Override
    public ApiResponse<?> send(ApiRequest request) throws IOException {
        String response = connection.send(JsonCodec.toJson(request));
        return JsonCodec.fromJson(response, request.operation());
    }

    @Override
    public void close() throws IOException {
        connection.close();
    }
}
