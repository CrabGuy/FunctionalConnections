package shared.dto;

/**
 * Request to log in.
 *
 * @param operation the operation name ("login")
 * @param username the username
 * @param psw the password
 * @param udpPort the UDP port for notifications
 */
public record LoginRequest(String operation, String username, String psw, int udpPort)
    implements ApiRequest {

  /**
   * Convenience constructor that sets the operation.
   *
   * @param username the username
   * @param psw the password
   * @param udpPort the UDP port
   */
  public LoginRequest(String username, String psw, int udpPort) {
    this("login", username, psw, udpPort);
  }
}
