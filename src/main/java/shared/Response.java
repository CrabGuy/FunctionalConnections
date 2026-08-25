package shared;

public record Response<T>(boolean success, T result, String error) {
    public static <T> Response<T> success(T result) {
        return new Response<>(true, result, null);
    }
    public static <T> Response<T> error(ErrorCode code) {
        return new Response<>(false, null, code.code());
    }
    public static <T> Response<T> error(String message) {
        return new Response<>(false, null, message);
    }
}