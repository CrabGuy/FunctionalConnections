package server.game.exceptions;

import shared.dto.ErrorCode;

/** Thrown when a proposal contains words that have already been correctly grouped. */
public final class WordsAlreadyGroupedException extends InvalidProposalException {

  /** Constructs the exception. */
  public WordsAlreadyGroupedException() {
    super(
        ErrorCode.WORDS_ALREADY_GROUPED,
        "One or more words in the proposal have already been grouped.");
  }
}
