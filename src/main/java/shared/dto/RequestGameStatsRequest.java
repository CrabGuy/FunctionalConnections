package shared.dto;

/**
 * Request to fetch game statistics.
 *
 * @param operation the operation name ("requestGameStats")
 * @param accountToken the account token
 * @param gameId optional game ID, or {@code null} for current game
 */
public record RequestGameStatsRequest(String operation, String accountToken, Long gameId)
    implements ApiRequest {

  /**
   * Convenience constructor that sets the operation.
   *
   * @param accountToken the account token
   * @param gameId the game ID or null
   */
  public RequestGameStatsRequest(String accountToken, Long gameId) {
    this("requestGameStats", accountToken, gameId);
  }
}
