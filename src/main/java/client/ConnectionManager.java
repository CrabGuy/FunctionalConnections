package client;

import shared.dto.ApiRequest;
import shared.dto.ApiResponse;
import java.io.IOException;

/**
 * Manages the persistent TCP connection to the server and provides a blocking
 * request/response mechanism.
 *
 * <p>This interface abstracts the transport layer. Implementations are expected
 * to use Java NIO to maintain a persistent channel, serialize requests as JSON,
 * and deserialize responses according to the {@link ApiResponse} hierarchy.</p>
 *
 * <p>All methods may throw {@link IOException} if the connection is lost,
 * malformed data is received, or the server closes the channel unexpectedly.</p>
 */
public interface ConnectionManager {

    /**
     * Establishes a persistent TCP connection to the specified server address.
     *
     * @param host the server hostname or IP address
     * @param port the server TCP port
     * @throws IOException if the connection cannot be established
     */
    void connect(String host, int port) throws IOException;

    /**
     * Sends a request to the server and blocks until a response is received.
     *
     * <p>The request is serialized to JSON, transmitted over the persistent
     * connection, and the server's response is deserialized into an
     * {@link ApiResponse}. The returned response is guaranteed to match the
     * operation of the original request (i.e., the generic type parameter is
     * consistent with the request's expected response type).</p>
     *
     * @param request the request object to send
     * @return the server's response, never {@code null}
     * @throws IOException if the request cannot be sent, the connection is lost,
     *                     or the response cannot be parsed
     */
    ApiResponse<?> send(ApiRequest request) throws IOException;

    /**
     * Closes the persistent TCP connection and releases associated resources.
     *
     * @throws IOException if an I/O error occurs while closing the channel
     */
    void close() throws IOException;
}