package server.account.exceptions;

import shared.dto.ErrorCode;

public abstract sealed class AccountException extends RuntimeException
    permits IncorrectPasswordException,
        InvalidTokenException,
        NewUsernameAlreadyTakenException,
        UsernameAlreadyRegisteredException {

  private final ErrorCode errorCode;

  protected AccountException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public ErrorCode errorCode() {
    return errorCode;
  }
}
