package server.game.exceptions;

import shared.dto.ErrorCode;

/**
 * Thrown when a submitted proposal does not contain exactly 4 valid words.
 */
public final class MalformedProposalException extends InvalidProposalException {
    public MalformedProposalException() {
        super(ErrorCode.MALFORMED_PROPOSAL, "Proposal must contain exactly 4 words.");
    }
}