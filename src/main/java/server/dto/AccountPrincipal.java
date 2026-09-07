package server.dto;

/**
 * Represents the principal extracted from a verified account token.
 *
 * @param username the username
 * @param expiresAt the token expiration timestamp in milliseconds
 */
public record AccountPrincipal(String username, long expiresAt) {}
