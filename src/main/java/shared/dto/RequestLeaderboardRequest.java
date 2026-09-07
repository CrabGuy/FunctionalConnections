package shared.dto;

/**
 * Request to fetch the leaderboard.
 *
 * @param operation the operation name ("requestLeaderboard")
 * @param accountToken the account token
 * @param playerName optional specific player name, or {@code null}
 * @param topPlayers optional number of top players, or {@code null} for all
 */
public record RequestLeaderboardRequest(
    String operation, String accountToken, String playerName, Integer topPlayers)
    implements ApiRequest {

  /**
   * Convenience constructor that sets the operation.
   *
   * @param accountToken the account token
   * @param playerName the player name or null
   * @param topPlayers the number of top players or null
   */
  public RequestLeaderboardRequest(String accountToken, String playerName, Integer topPlayers) {
    this("requestLeaderboard", accountToken, playerName, topPlayers);
  }
}
