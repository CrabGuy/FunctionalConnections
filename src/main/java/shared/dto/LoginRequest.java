package shared.dto;

/**
 * Login request. {@code udpPort} is the port the client is listening on
 * for asynchronous UDP notifications (e.g. game-end); the server pairs it
 * with the TCP connection's source address to register the client for
 * notifications on successful login.
 */
public record LoginRequest(String username, String password, int udpPort) implements ApiRequest {
    @Override
    public String getOperation() {
        return "login";
    }
}
