package shared.dto;

/**
 * Request to register a new account.
 *
 * @param operation the operation name ("register")
 * @param username the desired username
 * @param psw the desired password
 */
public record RegisterRequest(String operation, String username, String psw) implements ApiRequest {

  /**
   * Convenience constructor that sets the operation.
   *
   * @param username the username
   * @param psw the password
   */
  public RegisterRequest(String username, String psw) {
    this("register", username, psw);
  }
}
