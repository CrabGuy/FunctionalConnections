package server.account.exceptions;
import shared.dto.ErrorCode;
public final class InvalidTokenException extends AccountException {
    public InvalidTokenException(String reason) {
        super(ErrorCode.USER_NOT_LOGGED_IN, "Invalid account token: " + reason);
    }
}
