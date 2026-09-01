package shared.dto;

/**
 * Generic envelope for all server responses.
 * Contains a success flag, an optional error object (if success is false),
 * and optional data (if success is true).
 *
 * @param <T> type of the data payload (specific to each operation)
 */
public record ApiResponse<T>(boolean success, ApiError error, T data) {
}
