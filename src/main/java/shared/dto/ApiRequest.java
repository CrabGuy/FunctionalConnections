package shared.dto;

/** Marker interface for all API request DTOs. Each request must have an operation name. */
public sealed interface ApiRequest
    permits RegisterRequest,
        LoginRequest,
        LogoutRequest,
        UpdateCredentialsRequest,
        SubmitProposalRequest,
        RequestGameInfoRequest,
        RequestGameStatsRequest,
        RequestLeaderboardRequest,
        RequestPlayerStatsRequest {

  /**
   * Returns the operation name of the request.
   *
   * @return the operation name
   */
  String operation();
}
