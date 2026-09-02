package server.account.exceptions;
import shared.dto.ErrorCode;
public final class NewUsernameAlreadyTakenException extends AccountException {
    public NewUsernameAlreadyTakenException(String newUsername) {
        super(ErrorCode.NEW_USERNAME_ALREADY_TAKEN, "Username already taken: " + newUsername);
    }
}
