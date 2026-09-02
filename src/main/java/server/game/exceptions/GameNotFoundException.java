package server.game.exceptions;
import shared.dto.ErrorCode;
public final class GameNotFoundException extends GameException {
    public GameNotFoundException(long gameId) {
        super(ErrorCode.GAME_NOT_FOUND, "No game found for id: " + gameId);
    }
}
