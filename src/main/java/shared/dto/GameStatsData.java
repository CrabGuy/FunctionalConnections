package shared.dto;

/**
 * Aggregate statistics for a specific game.
 *
 * @param gameId the game ID
 * @param completed true if the game has ended
 * @param expiresAt the expiration time in milliseconds
 * @param totalParticipants the total number of participants
 * @param activePlayers the number of active players
 * @param completedPlayers the number of players who completed (won or lost)
 * @param winners the number of winners
 * @param averageScore the average score among participants
 */
public record GameStatsData(
    long gameId,
    boolean completed,
    long expiresAt,
    int totalParticipants,
    int activePlayers,
    int completedPlayers,
    int winners,
    double averageScore) {}
