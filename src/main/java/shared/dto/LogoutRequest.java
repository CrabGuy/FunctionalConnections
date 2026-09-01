package shared.dto;

/**
 * Request for user logout.
 * Operation: "logout"
 *
 * @param accountToken the JWT token identifying the logged-in user.
 */
public record LogoutRequest(String accountToken) implements ApiRequest {
    @Override
    public String getOperation() {
        return "logout";
    }
}
