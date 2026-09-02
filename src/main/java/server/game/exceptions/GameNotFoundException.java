package server.game.exceptions;

import shared.dto.ErrorCode;

/**
 * Thrown by {@link server.game.GameRepository#loadById(long)} when the
 * given game identifier cannot be resolved to a catalog entry (for
 * example, an empty catalog).
 */
public final class GameNotFoundException extends GameException {
    public GameNotFoundException(long gameId) {
        super(ErrorCode.GAME_NOT_FOUND, "No game found for id: " + gameId);
    }
}