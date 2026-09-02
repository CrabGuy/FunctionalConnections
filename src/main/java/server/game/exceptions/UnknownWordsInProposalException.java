package server.game.exceptions;

import shared.dto.ErrorCode;

/**
 * Thrown when a proposal contains words that do not exist in the current game.
 */
public final class UnknownWordsInProposalException extends InvalidProposalException {
    public UnknownWordsInProposalException() {
        super(ErrorCode.UNKNOWN_WORDS_IN_PROPOSAL, "Proposal contains words not present in the current game.");
    }
}