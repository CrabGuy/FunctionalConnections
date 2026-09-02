package server.account.exceptions;

import shared.dto.ErrorCode;

/**
 * Thrown by {@link AccountService#updateCredentials} when the requested
 * new username is already registered to a different account.
 */
public final class NewUsernameAlreadyTakenException extends AccountException {
    public NewUsernameAlreadyTakenException(String newUsername) {
        super(ErrorCode.NEW_USERNAME_ALREADY_TAKEN, "Username already taken: " + newUsername);
    }
}