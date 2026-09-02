package shared.dto;
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
