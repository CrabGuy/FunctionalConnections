package shared.dto;
public record ApiResponse<T>(boolean success, ApiError error, T data) {
}
