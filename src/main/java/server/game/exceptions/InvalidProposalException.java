package server.game.exceptions;

import shared.dto.ErrorCode;

public abstract class InvalidProposalException extends GameException {
  protected InvalidProposalException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
