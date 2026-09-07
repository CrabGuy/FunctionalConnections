package shared.dto;

/**
 * Aggregate statistics for a player across all games.
 *
 * @param puzzlesCompleted number of completed puzzles
 * @param winRate fraction of wins (0.0 to 1.0)
 * @param lossRate fraction of losses (0.0 to 1.0)
 * @param currentStreak current winning streak
 * @param maxStreak maximum winning streak
 * @param perfectPuzzles number of wins with zero mistakes
 * @param mistakeHistogram distribution of wins by mistake count
 */
public record PlayerStatsData(
    int puzzlesCompleted,
    double winRate,
    double lossRate,
    int currentStreak,
    int maxStreak,
    int perfectPuzzles,
    MistakeHistogram mistakeHistogram) {}
