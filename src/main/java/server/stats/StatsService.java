package server.stats;

import server.account.exceptions.InvalidTokenException;
import server.game.exceptions.GameNotFoundException;
import shared.dto.GameStatsData;
import shared.dto.PlayerStatsData;

public interface StatsService {
  GameStatsData getGameStats(String accountToken, Long gameId)
      throws GameNotFoundException, InvalidTokenException;

  PlayerStatsData getPlayerStats(String accountToken) throws InvalidTokenException;
}
