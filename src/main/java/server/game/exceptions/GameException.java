package server.game.exceptions;
import shared.dto.ErrorCode;
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
