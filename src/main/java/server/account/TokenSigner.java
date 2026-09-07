package server.account;

import server.account.exceptions.InvalidTokenException;
import server.dto.AccountPrincipal;

/** Interface for signing and verifying account tokens. */
public interface TokenSigner {

  /**
   * Signs a username and expiration time into a token.
   *
   * @param username the username
   * @param expiresAt the expiration timestamp in milliseconds
   * @return the signed token
   */
  String sign(String username, long expiresAt);

  /**
   * Verifies a token and returns the principal.
   *
   * @param token the token
   * @return the account principal
   * @throws InvalidTokenException if the token is invalid or expired
   */
  AccountPrincipal verify(String token) throws InvalidTokenException;
}
