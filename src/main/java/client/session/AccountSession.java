package client.session;

/** Holds the current user's account token and provides session state management. */
public final class AccountSession {

  private volatile String accountToken;

  /**
   * Returns the current account token.
   *
   * @return the account token, or {@code null} if not logged in
   */
  public String accountToken() {
    return accountToken;
  }

  /**
   * Sets the account token.
   *
   * @param accountToken the new account token
   */
  public void setAccountToken(String accountToken) {
    this.accountToken = accountToken;
  }

  /** Clears the account token, effectively logging out. */
  public void clear() {
    this.accountToken = null;
  }

  /**
   * Checks whether the user is logged in (i.e., a non-blank token is present).
   *
   * @return true if logged in, false otherwise
   */
  public boolean isLoggedIn() {
    return accountToken != null && !accountToken.isBlank();
  }
}
