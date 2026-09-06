package server.game.exceptions;

import shared.dto.ErrorCode;

public final class PlayerAlreadyCompletedGameException extends GameException {
  public PlayerAlreadyCompletedGameException(String username, long gameId) {
    super(
        ErrorCode.PLAYER_ALREADY_COMPLETED_GAME,
        "Player " + username + " has already completed game " + gameId);
  }
}
