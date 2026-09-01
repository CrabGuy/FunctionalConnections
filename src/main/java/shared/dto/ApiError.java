package shared.dto;

/**
 * Represents an error response sent by the server.
 * Contains a standard error code and a human-readable message.
 */
public record ApiError(ErrorCode code, String message) {
}
