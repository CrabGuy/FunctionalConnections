package server.game.exceptions;

import shared.dto.ErrorCode;

public abstract sealed class InvalidProposalException extends GameException
    permits MalformedProposalException,
        UnknownWordsInProposalException,
        WordsAlreadyGroupedException {

  protected InvalidProposalException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
