package server.account.exceptions;

import shared.dto.ErrorCode;

/** Thrown when trying to register a username that already exists. */
public final class UsernameAlreadyRegisteredException extends AccountException {

  /**
   * Constructs the exception for a given username.
   *
   * @param username the username
   */
  public UsernameAlreadyRegisteredException(String username) {
    super(ErrorCode.USERNAME_ALREADY_REGISTERED, "Username already registered: " + username);
  }
}
