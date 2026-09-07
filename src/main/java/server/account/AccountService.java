package server.account;

import server.account.exceptions.IncorrectPasswordException;
import server.account.exceptions.InvalidTokenException;
import server.account.exceptions.NewUsernameAlreadyTakenException;
import server.account.exceptions.UsernameAlreadyRegisteredException;
import server.dto.AccountPrincipal;
import shared.dto.LoginData;
import shared.dto.RegisterData;
import shared.dto.UpdateCredentialsData;

/**
 * Defines the account management operations such as registration, login, logout, credential
 * updates, and token resolution.
 */
public interface AccountService {

  /**
   * Registers a new account.
   *
   * @param username the desired username
   * @param password the desired password
   * @return registration data
   * @throws UsernameAlreadyRegisteredException if the username is already taken
   */
  RegisterData register(String username, String password) throws UsernameAlreadyRegisteredException;

  /**
   * Logs in a user and registers their UDP notification address.
   *
   * @param username the username
   * @param password the password
   * @param udpPort the UDP port for notifications
   * @param remoteAddress the client's IP address
   * @return login data containing the account token
   * @throws IncorrectPasswordException if credentials are invalid
   */
  LoginData login(String username, String password, int udpPort, String remoteAddress)
      throws IncorrectPasswordException;

  /**
   * Logs out a user by invalidating their session and unregistering notification address.
   *
   * @param accountToken the account token
   * @throws InvalidTokenException if the token is invalid
   */
  void logout(String accountToken) throws InvalidTokenException;

  /**
   * Updates a user's credentials (username and/or password).
   *
   * @param oldUsername the current username
   * @param newUsername the new username (or blank/unchanged)
   * @param oldPassword the current password
   * @param newPassword the new password (or blank/unchanged)
   * @return the updated credentials data
   * @throws IncorrectPasswordException if old password is incorrect
   * @throws NewUsernameAlreadyTakenException if the new username is already taken
   */
  UpdateCredentialsData updateCredentials(
      String oldUsername, String newUsername, String oldPassword, String newPassword)
      throws IncorrectPasswordException, NewUsernameAlreadyTakenException;

  /**
   * Resolves an account token to a principal.
   *
   * @param accountToken the account token
   * @return the account principal
   * @throws InvalidTokenException if the token is invalid
   */
  AccountPrincipal resolve(String accountToken) throws InvalidTokenException;
}
