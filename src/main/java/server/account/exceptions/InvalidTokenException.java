package server.account.exceptions;

import shared.dto.ErrorCode;

/** Thrown when an account token is invalid or expired. */
public final class InvalidTokenException extends AccountException {

  /**
   * Constructs the exception with a reason.
   *
   * @param reason the reason for invalidity
   */
  public InvalidTokenException(String reason) {
    super(ErrorCode.USER_NOT_LOGGED_IN, "Invalid account token: " + reason);
  }
}
