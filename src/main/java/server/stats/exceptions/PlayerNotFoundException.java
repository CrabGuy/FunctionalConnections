package server.stats.exceptions;

import shared.dto.ErrorCode;

public final class PlayerNotFoundException extends RuntimeException {
  private final ErrorCode errorCode;

  public PlayerNotFoundException(String playerName) {
    super("Player not found: " + playerName);
    this.errorCode = ErrorCode.PLAYER_NOT_FOUND;
  }

  public ErrorCode errorCode() {
    return errorCode;
  }
}