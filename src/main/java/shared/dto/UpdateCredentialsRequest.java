package shared.dto;

/**
 * Request to update username and/or password.
 * Operation: "updateCredentials"
 */
public record UpdateCredentialsRequest(
        String oldUsername,
        String newUsername,
        String oldPassword,
        String newPassword
) implements ApiRequest {
    @Override
    public String getOperation() {
        return "updateCredentials";
    }
}
