package server.game;

import server.dto.GameWordGroups;
import server.game.exceptions.GameNotFoundException;

public interface GameRepository {
  GameWordGroups loadById(long gameId) throws GameNotFoundException;

  boolean exists(long gameId);
}
