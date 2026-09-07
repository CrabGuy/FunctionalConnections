package client.connection;

import java.io.IOException;
import shared.dto.ApiRequest;
import shared.dto.ApiResponse;

/**
 * Defines the contract for a client-side connection manager that handles establishing connections,
 * sending requests, and closing the connection.
 */
public interface ConnectionManager {

  /**
   * Connects to the specified server address and port.
   *
   * @param host the server host name or IP address
   * @param port the server TCP port
   * @throws IOException if an I/O error occurs during connection
   */
  void connect(String host, int port) throws IOException;

  /**
   * Sends a request to the server and returns the response.
   *
   * @param request the request to send
   * @return the server response
   * @throws IOException if an I/O error occurs during communication
   */
  ApiResponse<?> send(ApiRequest request) throws IOException;

  /**
   * Closes the connection and releases resources.
   *
   * @throws IOException if an I/O error occurs while closing
   */
  void close() throws IOException;
}
