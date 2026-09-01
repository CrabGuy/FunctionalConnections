package server.dto;

/**
 * Stores user credentials for authentication.
 * Immutable; password is stored only as a hash.
 *
 * @param username     the user's username.
 * @param passwordHash the hashed password.
 */
public record PlayerCredentials(String username, String passwordHash) {
}
