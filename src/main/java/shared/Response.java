package shared;

public record Response(boolean success, String result, String error) {
    public static Response success(String result) {
        return new Response(true, result, null);
    }

    public static Response error(ErrorCode code) {
        return new Response(false, null, code.code());
    }

    public static Response error(String code) {
        return new Response(false, null, code);
    }
}