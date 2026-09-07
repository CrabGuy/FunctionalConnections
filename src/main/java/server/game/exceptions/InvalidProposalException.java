package server.game.exceptions;

import shared.dto.ErrorCode;

/** Base class for exceptions related to invalid proposals. */
public abstract sealed class InvalidProposalException extends GameException
    permits MalformedProposalException,
        UnknownWordsInProposalException,
        WordsAlreadyGroupedException {

  /**
   * Constructs the exception.
   *
   * @param errorCode the error code
   * @param message the detail message
   */
  protected InvalidProposalException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
