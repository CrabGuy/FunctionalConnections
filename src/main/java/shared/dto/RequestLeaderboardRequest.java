package shared.dto;
public record RequestLeaderboardRequest(String accountToken, String playerName, Integer topPlayers) implements ApiRequest {
    @Override
    public String getOperation() {
        return "requestLeaderboard";
    }
}
