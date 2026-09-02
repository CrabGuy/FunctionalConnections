package server.account.exceptions;

import shared.dto.ErrorCode;

/**
 * Thrown by {@link AccountService#login} and {@link AccountService#updateCredentials}
 * when the supplied password does not match the stored hash for the given
 * username.
 */
public final class IncorrectPasswordException extends AccountException {
    public IncorrectPasswordException(String username) {
        super(ErrorCode.INCORRECT_PASSWORD, "Incorrect password for username: " + username);
    }
}