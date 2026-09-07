package server.game;

import server.dto.GameWordGroups;
import server.game.exceptions.GameNotFoundException;

/** Repository for loading game definitions. */
public interface GameRepository {

  /**
   * Loads the game word groups for the given game ID.
   *
   * @param gameId the game ID
   * @return the game word groups
   * @throws GameNotFoundException if the game does not exist
   */
  GameWordGroups loadById(long gameId) throws GameNotFoundException;

  /**
   * Checks if a game with the given ID exists.
   *
   * @param gameId the game ID
   * @return true if exists, false otherwise
   */
  boolean exists(long gameId);
}
