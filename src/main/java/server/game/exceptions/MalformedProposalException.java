package server.game.exceptions;

import shared.dto.ErrorCode;

public final class MalformedProposalException extends InvalidProposalException {
  public MalformedProposalException() {
    super(ErrorCode.MALFORMED_PROPOSAL, "Proposal must contain exactly 4 words.");
  }
}
