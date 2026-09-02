package server.stats;
import shared.dto.LeaderboardData;
import server.account.exceptions.InvalidTokenException;
public interface LeaderboardService {
    LeaderboardData getLeaderboard(String accountToken, String playerName, Integer topK) throws InvalidTokenException;
}
