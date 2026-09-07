package shared.dto;

/**
 * Request to fetch game information.
 *
 * @param operation the operation name ("requestGameInfo")
 * @param accountToken the account token
 * @param gameId optional game ID, or {@code null} for current game
 */
public record RequestGameInfoRequest(String operation, String accountToken, Long gameId)
    implements ApiRequest {

  /**
   * Convenience constructor that sets the operation.
   *
   * @param accountToken the account token
   * @param gameId the game ID or null
   */
  public RequestGameInfoRequest(String accountToken, Long gameId) {
    this("requestGameInfo", accountToken, gameId);
  }
}
