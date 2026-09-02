package server.account.exceptions;
import shared.dto.ErrorCode;
public final class UsernameAlreadyRegisteredException extends AccountException {
    public UsernameAlreadyRegisteredException(String username) {
        super(ErrorCode.USERNAME_ALREADY_REGISTERED, "Username already registered: " + username);
    }
}
