package server.dto;

/**
 * Represents a user account with a username and password hash.
 *
 * @param username the username
 * @param passwordHash the hashed password
 */
public record Account(String username, String passwordHash) {}
