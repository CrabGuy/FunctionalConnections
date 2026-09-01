package shared.dto;

import java.util.List;

/**
 * Data payload for the "requestLeaderboard" operation.
 * Can contain either top K players, a specific player's entry, or the full leaderboard.
 *
 * @param topPlayers       list of top K entries (empty if not requested).
 * @param requestedPlayer  entry for the specifically requested player (null if not requested).
 * @param totalPlayers     total number of players in the leaderboard.
 */
public record LeaderboardData(
        List<LeaderboardEntry> topPlayers,
        LeaderboardEntry requestedPlayer,
        int totalPlayers
) {
    public LeaderboardData {
        topPlayers = List.copyOf(topPlayers);
    }
}
