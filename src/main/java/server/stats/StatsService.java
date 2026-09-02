package server.stats;
import shared.dto.GameStatsData;
import shared.dto.PlayerStatsData;
import server.account.exceptions.InvalidTokenException;
import server.game.exceptions.GameNotFoundException;
public interface StatsService {
    GameStatsData getGameStats(String accountToken, Long gameId) throws GameNotFoundException, InvalidTokenException;
    PlayerStatsData getPlayerStats(String accountToken) throws InvalidTokenException;
}
