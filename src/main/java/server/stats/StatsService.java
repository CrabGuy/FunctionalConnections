package server.stats;

import server.account.exceptions.InvalidTokenException;
import server.game.exceptions.GameNotFoundException;
import shared.dto.GameStatsData;
import shared.dto.PlayerStatsData;

/** Service for retrieving game statistics and player statistics. */
public interface StatsService {

  /**
   * Retrieves statistics for a specified or current game.
   *
   * @param accountToken the account token
   * @param gameId the game ID, or {@code null} for current game
   * @return the game statistics
   * @throws GameNotFoundException if the game does not exist
   * @throws InvalidTokenException if the token is invalid
   */
  GameStatsData getGameStats(String accountToken, Long gameId)
      throws GameNotFoundException, InvalidTokenException;

  /**
   * Retrieves aggregate statistics for the logged-in player.
   *
   * @param accountToken the account token
   * @return the player statistics
   * @throws InvalidTokenException if the token is invalid
   */
  PlayerStatsData getPlayerStats(String accountToken) throws InvalidTokenException;
}
