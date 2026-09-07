package server.stats;

import server.account.exceptions.InvalidTokenException;
import server.stats.exceptions.PlayerNotFoundException;
import shared.dto.LeaderboardData;

/** Service for generating leaderboards. */
public interface LeaderboardService {

  /**
   * Retrieves the leaderboard, optionally filtered by player name or limited to top N.
   *
   * @param accountToken the account token
   * @param playerName optional specific player name
   * @param topK optional number of top players
   * @return the leaderboard data
   * @throws InvalidTokenException if the token is invalid
   * @throws PlayerNotFoundException if the requested player is not found
   */
  LeaderboardData getLeaderboard(String accountToken, String playerName, Integer topK)
      throws InvalidTokenException, PlayerNotFoundException;
}
