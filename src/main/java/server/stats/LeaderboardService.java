package server.stats;

import server.account.exceptions.InvalidTokenException;
import shared.dto.LeaderboardData;

public interface LeaderboardService {
  LeaderboardData getLeaderboard(String accountToken, String playerName, Integer topK)
      throws InvalidTokenException;
}
