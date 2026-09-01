package shared.dto;

/**
 * Request for user registration.
 * Operation: "register"
 */
public record RegisterRequest(String username, String password) implements ApiRequest {
    @Override
    public String getOperation() {
        return "register";
    }
}
