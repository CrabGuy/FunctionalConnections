package server.stats;

import shared.dto.LeaderboardData;
import server.account.exceptions.InvalidTokenException;

/**
 * Service responsible for providing leaderboard rankings.
 * Players should be ranked in leaderboard based on score.
 */
public interface LeaderboardService {

    /**
     * Generates a leaderboard based on player scores.
     * Can request relative ranking or top K users.
     *
     * @param accountToken the access token of the requesting player, formatted as a JWT.
     * @param playerName an optional specific username to lookup in the leaderboard.
     * @param topK the number of top users to retrieve. Option to specify all players.
     * @return LeaderboardData containing the top players, the requested player's entry, and total players.
     * @throws InvalidTokenException if the account token is missing, invalid, or expired.
     */
    LeaderboardData getLeaderboard(String accountToken, String playerName, Integer topK) throws InvalidTokenException;
}