package server.account.exceptions;

import shared.dto.ErrorCode;

/**
 * Base type for all account-related domain failures. Carries the
 * {@link ErrorCode} that Slice E's {@code RequestDispatcher} should use
 * when translating a thrown exception into an {@code ApiResponse}.
 * Unchecked, so service interfaces stay free of {@code throws} clauses —
 * callers that care catch the specific subtype or this base type.
 */
public abstract class AccountException extends RuntimeException {

    private final ErrorCode errorCode;

    protected AccountException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}