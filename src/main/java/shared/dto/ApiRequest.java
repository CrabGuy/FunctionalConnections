package shared.dto;

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
  String operation();
}
