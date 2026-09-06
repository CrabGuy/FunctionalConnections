package shared.dto;

public record UpdateCredentialsRequest(
    String operation, String oldUsername, String newUsername, String oldPsw, String newPsw)
    implements ApiRequest {
  public UpdateCredentialsRequest(
      String oldUsername, String newUsername, String oldPsw, String newPsw) {
    this("updateCredentials", oldUsername, newUsername, oldPsw, newPsw);
  }
}
