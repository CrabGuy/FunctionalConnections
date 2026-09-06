package server.account.exceptions;

import shared.dto.ErrorCode;

public final class IncorrectPasswordException extends AccountException {
  public IncorrectPasswordException(String username) {
    super(ErrorCode.INCORRECT_PASSWORD, "Incorrect password for username: " + username);
  }
}
