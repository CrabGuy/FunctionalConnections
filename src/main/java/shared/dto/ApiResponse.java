package shared.dto;

/**
 * Generic API response wrapper.
 *
 * @param success true if the request succeeded, false otherwise
 * @param error the error details if unsuccessful, otherwise {@code null}
 * @param data the response data if successful, otherwise {@code null}
 * @param <T> the type of the data
 */
public record ApiResponse<T>(boolean success, ApiError error, T data) {}
