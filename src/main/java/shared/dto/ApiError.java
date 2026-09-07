package shared.dto;

/**
 * Represents an error in an API response.
 *
 * @param code the error code
 * @param message the human-readable error message
 */
public record ApiError(ErrorCode code, String message) {}
