package server.account.exceptions;

import shared.dto.ErrorCode;

/** Thrown when trying to update to a username that is already registered. */
public final class NewUsernameAlreadyTakenException extends AccountException {

  /**
   * Constructs the exception for a given new username.
   *
   * @param newUsername the proposed new username
   */
  public NewUsernameAlreadyTakenException(String newUsername) {
    super(ErrorCode.NEW_USERNAME_ALREADY_TAKEN, "Username already taken: " + newUsername);
  }
}
