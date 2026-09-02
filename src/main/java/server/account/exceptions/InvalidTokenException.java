package server.account.exceptions;

import shared.dto.ErrorCode;

/**
 * Thrown by {@link TokenSigner#verify}, {@link AccountService#resolve},
 * and {@link AccountService#logout} when a token is malformed, has an
 * invalid signature, or is expired.
 *
 * <p>Mapped to {@link ErrorCode#USER_NOT_LOGGED_IN}.
 */
public final class InvalidTokenException extends AccountException {
    public InvalidTokenException(String reason) {
        super(ErrorCode.USER_NOT_LOGGED_IN, "Invalid account token: " + reason);
    }
}