package shared.dto;

/**
 * Common interface for all client-to-server request messages.
 * Each concrete request record must implement this interface
 * and provide the operation name as a constant string.
 */
public interface ApiRequest {
    /**
     * Returns the operation name as defined in the communication protocol.
     * @return the operation string (e.g., "register", "login")
     */
    String getOperation();
}
