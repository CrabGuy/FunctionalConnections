package server.dto;

/**
 * Composite key for a player-game pair.
 *
 * @param username the username
 * @param gameId the game ID
 */
public record PlayerGameKey(String username, long gameId) {}
