package client;
import shared.dto.ApiRequest;
import shared.dto.ApiResponse;
import java.io.IOException;
public interface ConnectionManager {
    void connect(String host, int port) throws IOException;
    ApiResponse<?> send(ApiRequest request) throws IOException;
    void close() throws IOException;
}
