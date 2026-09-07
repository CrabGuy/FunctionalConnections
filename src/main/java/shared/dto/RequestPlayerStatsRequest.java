package shared.dto;

/**
 * Request to fetch player statistics.
 *
 * @param operation the operation name ("requestPlayerStats")
 * @param accountToken the account token
 */
public record RequestPlayerStatsRequest(String operation, String accountToken)
    implements ApiRequest {

  /**
   * Convenience constructor that sets the operation.
   *
   * @param accountToken the account token
   */
  public RequestPlayerStatsRequest(String accountToken) {
    this("requestPlayerStats", accountToken);
  }
}
