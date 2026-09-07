package client.connection;

import client.json.JsonCodec;
import java.io.IOException;
import shared.dto.ApiRequest;
import shared.dto.ApiResponse;

/**
 * Implementation of {@link ConnectionManager} that uses a {@link TcpConnection} for NIO-based
 * communication and JSON serialization.
 */
public record NioConnectionManager(TcpConnection connection) implements ConnectionManager {

  /** Creates a new instance with a fresh {@link TcpConnection}. */
  public NioConnectionManager() {
    this(new TcpConnection());
  }

  /** {@inheritDoc} */
  @Override
  public void connect(String host, int port) throws IOException {
    connection.connect(host, port);
  }

  /** {@inheritDoc} */
  @Override
  public ApiResponse<?> send(ApiRequest request) throws IOException {
    String response = connection.send(JsonCodec.toJson(request));
    return JsonCodec.fromJson(response, request.operation());
  }

  /** {@inheritDoc} */
  @Override
  public void close() throws IOException {
    connection.close();
  }
}
