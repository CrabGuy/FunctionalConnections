package shared.dto;

public record RequestGameStatsRequest(String operation, String accountToken, Long gameId)
    implements ApiRequest {
  public RequestGameStatsRequest(String accountToken, Long gameId) {
    this("requestGameStats", accountToken, gameId);
  }
}
