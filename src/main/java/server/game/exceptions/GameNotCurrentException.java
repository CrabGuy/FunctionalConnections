package server.game.exceptions;

import shared.dto.ErrorCode;

/** Thrown when an operation is attempted on a game that is not the currently active game. */
public final class GameNotCurrentException extends GameException {

  /**
   * Constructs the exception.
   *
   * @param requestedGameId the requested game ID
   * @param currentGameId the current game ID
   */
  public GameNotCurrentException(long requestedGameId, long currentGameId) {
    super(
        ErrorCode.GAME_NOT_CURRENT,
        "Game "
            + requestedGameId
            + " is not the currently active game (current: "
            + currentGameId
            + ")");
  }
}
