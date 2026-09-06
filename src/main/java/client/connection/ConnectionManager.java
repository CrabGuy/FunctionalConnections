package client.connection;

import java.io.IOException;
import shared.dto.ApiRequest;
import shared.dto.ApiResponse;

public interface ConnectionManager {
  void connect(String host, int port) throws IOException;

  ApiResponse<?> send(ApiRequest request) throws IOException;

  void close() throws IOException;
}
