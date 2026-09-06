package server.game.exceptions;

import shared.dto.ErrorCode;

public final class GameNotCurrentException extends GameException {
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
