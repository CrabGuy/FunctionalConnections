package shared.dto;

public record LogoutRequest(String operation, String accountToken) implements ApiRequest {
  public LogoutRequest(String accountToken) {
    this("logout", accountToken);
  }
}
