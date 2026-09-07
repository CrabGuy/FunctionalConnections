package shared.dto;

/**
 * Represents a notification sent to clients when a game ends.
 *
 * @param gameId the ID of the game that ended
 */
public record GameEndNotification(long gameId) {}
