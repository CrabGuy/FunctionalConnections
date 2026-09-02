package server.game.exceptions;

import shared.dto.ErrorCode;

/**
 * Base type for errors raised while resolving or loading games. Carries
 * the {@link ErrorCode} to be surfaced to clients in an {@code ApiError},
 * mirroring {@code server.account.exceptions.AccountException}.
 */
public abstract class GameException extends RuntimeException {

    private final ErrorCode errorCode;

    protected GameException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}