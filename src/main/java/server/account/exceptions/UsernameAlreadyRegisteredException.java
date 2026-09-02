package server.account.exceptions;

import shared.dto.ErrorCode;

/**
 * Thrown by {@link AccountService#register} when the requested username
 * is already taken.
 */
public final class UsernameAlreadyRegisteredException extends AccountException {
    public UsernameAlreadyRegisteredException(String username) {
        super(ErrorCode.USERNAME_ALREADY_REGISTERED, "Username already registered: " + username);
    }
}