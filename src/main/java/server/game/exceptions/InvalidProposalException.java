package server.game.exceptions;

import shared.dto.ErrorCode;

/**
 * Base exception for malformed or invalid proposals that do not impact the game's mistake counter.
 */
public abstract class InvalidProposalException extends GameException {
    protected InvalidProposalException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}