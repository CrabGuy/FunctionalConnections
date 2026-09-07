package server.game.exceptions;

import shared.dto.ErrorCode;

/** Thrown when a game with the specified ID does not exist. */
public final class GameNotFoundException extends GameException {

  /**
   * Constructs the exception.
   *
   * @param gameId the game ID that was not found
   */
  public GameNotFoundException(long gameId) {
    super(ErrorCode.GAME_NOT_FOUND, "No game found for id: " + gameId);
  }
}
