package server.game.exceptions;

import shared.dto.ErrorCode;

/** Thrown when a proposal does not contain exactly four distinct words. */
public final class MalformedProposalException extends InvalidProposalException {

  /** Constructs the exception. */
  public MalformedProposalException() {
    super(ErrorCode.MALFORMED_PROPOSAL, "Proposal must contain exactly 4 words.");
  }
}
