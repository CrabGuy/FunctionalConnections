package server.game.exceptions;

import shared.dto.ErrorCode;

/** Base class for game-related exceptions. */
public abstract sealed class GameException extends RuntimeException
    permits GameNotCurrentException,
        GameNotFoundException,
        InvalidProposalException,
        PlayerAlreadyCompletedGameException {

  private final ErrorCode errorCode;

  /**
   * Constructs a new game exception.
   *
   * @param errorCode the error code
   * @param message the detail message
   */
  protected GameException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  /**
   * Returns the error code.
   *
   * @return the error code
   */
  public ErrorCode errorCode() {
    return errorCode;
  }
}
