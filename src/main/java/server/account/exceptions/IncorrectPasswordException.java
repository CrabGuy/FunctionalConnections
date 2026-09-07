package server.account.exceptions;

import shared.dto.ErrorCode;

/** Thrown when an incorrect password is provided during login or credential update. */
public final class IncorrectPasswordException extends AccountException {

  /**
   * Constructs the exception for a given username.
   *
   * @param username the username
   */
  public IncorrectPasswordException(String username) {
    super(ErrorCode.INCORRECT_PASSWORD, "Incorrect password for username: " + username);
  }
}
