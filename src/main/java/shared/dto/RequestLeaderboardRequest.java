package shared.dto;

public record RequestLeaderboardRequest(
    String operation,
    String accountToken,
    String playerName,
    Integer topPlayers
) implements ApiRequest {
    public RequestLeaderboardRequest(String accountToken, String playerName, Integer topPlayers) {
        this("requestLeaderboard", accountToken, playerName, topPlayers);
    }
}
