package shared.dto;

/**
 * Represents the status of a player with respect to a specific game.
 */
public enum PlayerGameStatus {
    /** Player is currently active in the game (has not finished, game not ended). */
    ACTIVE,
    /** Player has completed the game successfully (guessed 3 groups). */
    WON,
    /** Player has lost (made 4 mistakes). */
    LOST,
    /** Game ended but player did not finish (neither won nor lost). */
    INCOMPLETE
}
