package server.stats.exceptions;

import shared.dto.ErrorCode;

/** Thrown when a requested player is not found in the leaderboard. */
public final class PlayerNotFoundException extends RuntimeException {

  private final ErrorCode errorCode;

  /**
   * Constructs the exception.
   *
   * @param playerName the requested player name
   */
  public PlayerNotFoundException(String playerName) {
    super("Player not found: " + playerName);
    this.errorCode = ErrorCode.PLAYER_NOT_FOUND;
  }

  /**
   * Returns the error code.
   *
   * @return the error code
   */
  public ErrorCode errorCode() {
    return errorCode;
  }
}
