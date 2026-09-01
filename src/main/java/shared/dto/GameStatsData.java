package shared.dto;

/**
 * Data payload for the "requestGameStats" operation.
 * Provides aggregated statistics about all players for a particular game.
 *
 * @param gameId             the ID of the game.
 * @param completed          true if the game period has ended.
 * @param remainingTimeSeconds number of seconds remaining; valid only if not completed.
 * @param totalParticipants  total number of players who participated (at least one proposal).
 * @param activePlayers      number of players currently active (not finished).
 * @param completedPlayers   number of players who have finished (won or lost).
 * @param winners            number of players who won the game.
 * @param averageScore       average score across all completed players; only if completed.
 */
public record GameStatsData(
        long gameId,
        boolean completed,
        int remainingTimeSeconds,
        int totalParticipants,
        int activePlayers,
        int completedPlayers,
        int winners,
        double averageScore
) {
}
