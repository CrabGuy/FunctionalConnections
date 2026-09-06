package shared.dto;

public record LoginRequest(String operation, String username, String psw, int udpPort)
    implements ApiRequest {
  public LoginRequest(String username, String psw, int udpPort) {
    this("login", username, psw, udpPort);
  }
}
