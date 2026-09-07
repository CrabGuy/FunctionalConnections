package server.account.exceptions;

import shared.dto.ErrorCode;

/** Base class for account-related exceptions. */
public abstract sealed class AccountException extends RuntimeException
    permits IncorrectPasswordException,
        InvalidTokenException,
        NewUsernameAlreadyTakenException,
        UsernameAlreadyRegisteredException {

  private final ErrorCode errorCode;

  /**
   * Constructs a new account exception with an error code and message.
   *
   * @param errorCode the error code
   * @param message the detail message
   */
  protected AccountException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
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
