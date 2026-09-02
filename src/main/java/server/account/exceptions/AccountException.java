package server.account.exceptions;
import shared.dto.ErrorCode;
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
