package shared.dto;

/**
 * Request to log out.
 *
 * @param operation the operation name ("logout")
 * @param accountToken the account token to invalidate
 */
public record LogoutRequest(String operation, String accountToken) implements ApiRequest {

  /**
   * Convenience constructor that sets the operation.
   *
   * @param accountToken the account token
   */
  public LogoutRequest(String accountToken) {
    this("logout", accountToken);
  }
}
