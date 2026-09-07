package server.game.exceptions;

import shared.dto.ErrorCode;

public abstract sealed class GameException extends RuntimeException
    permits GameNotCurrentException,
        GameNotFoundException,
        InvalidProposalException,
        PlayerAlreadyCompletedGameException {

  private final ErrorCode errorCode;

  protected GameException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public ErrorCode errorCode() {
    return errorCode;
  }
}
