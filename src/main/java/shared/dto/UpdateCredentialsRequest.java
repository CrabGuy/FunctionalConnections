package shared.dto;

/**
 * Request to update username and/or password.
 *
 * @param operation the operation name ("updateCredentials")
 * @param oldUsername the current username
 * @param newUsername the new username (or blank to keep)
 * @param oldPsw the current password
 * @param newPsw the new password (or blank to keep)
 */
public record UpdateCredentialsRequest(
    String operation, String oldUsername, String newUsername, String oldPsw, String newPsw)
    implements ApiRequest {

  /**
   * Convenience constructor that sets the operation.
   *
   * @param oldUsername the old username
   * @param newUsername the new username
   * @param oldPsw the old password
   * @param newPsw the new password
   */
  public UpdateCredentialsRequest(
      String oldUsername, String newUsername, String oldPsw, String newPsw) {
    this("updateCredentials", oldUsername, newUsername, oldPsw, newPsw);
  }
}
