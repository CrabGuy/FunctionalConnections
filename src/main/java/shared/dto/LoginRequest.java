package shared.dto;

/**
 * Request for user login.
 * Operation: "login"
 */
public record LoginRequest(String username, String password) implements ApiRequest {
    @Override
    public String getOperation() {
        return "login";
    }
}
