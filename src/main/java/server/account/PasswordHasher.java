package server.account;

/** Interface for password hashing and verification. */
public interface PasswordHasher {

  /**
   * Hashes a raw password.
   *
   * @param rawPassword the plaintext password
   * @return the hashed password representation
   */
  String hash(String rawPassword);

  /**
   * Checks if a raw password matches a stored hash.
   *
   * @param rawPassword the plaintext password
   * @param passwordHash the stored hash
   * @return true if matches, false otherwise
   */
  boolean matches(String rawPassword, String passwordHash);
}
