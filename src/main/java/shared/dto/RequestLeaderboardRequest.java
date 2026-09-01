package shared.dto;

/**
 * Request for leaderboard information.
 * Operation: "requestLeaderboard"
 *
 * @param playerName if provided, requests the ranking of this specific player.
 * @param topPlayers if greater than zero, requests the top K players.
 *                   If both are null/zero, requests the full leaderboard.
 */
public record RequestLeaderboardRequest(String playerName, Integer topPlayers) implements ApiRequest {
    @Override
    public String getOperation() {
        return "requestLeaderboard";
    }
}
