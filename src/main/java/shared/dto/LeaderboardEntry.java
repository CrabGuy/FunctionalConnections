package shared.dto;

/**
 * A single entry in the leaderboard.
 *
 * @param username the username
 * @param score the total score
 * @param rank the rank (1-based)
 */
public record LeaderboardEntry(String username, int score, int rank) {}
