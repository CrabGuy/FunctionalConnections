package shared.dto;

/**
 * A single entry in the leaderboard.
 *
 * @param username the player's username.
 * @param score    the player's total score.
 * @param rank     the player's rank (1-based).
 */
public record LeaderboardEntry(String username, int score, int rank) {
}
