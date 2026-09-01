package shared.dto;

/**
 * Request for user logout.
 * Operation: "logout"
 */
public record LogoutRequest() implements ApiRequest {
    @Override
    public String getOperation() {
        return "logout";
    }
}
