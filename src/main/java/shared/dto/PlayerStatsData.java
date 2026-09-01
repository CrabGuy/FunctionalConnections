package shared.dto;

import java.util.Map;

/**
 * Data payload for the "requestPlayerStats" operation.
 * Contains personal statistics for the authenticated player.
 *
 * @param puzzlesCompleted  total number of games completed (won or lost).
 * @param winRate           percentage of wins over completed games.
 * @param lossRate          percentage of losses over completed games.
 * @param currentStreak     current consecutive wins.
 * @param maxStreak         maximum consecutive wins.
 * @param perfectPuzzles    number of games won with zero mistakes.
 * @param mistakeHistogram  histogram of mistakes: keys are mistake counts (0-4, -1 for failure, -2 for incomplete),
 *                          values are counts.
 */
public record PlayerStatsData(
        int puzzlesCompleted,
        double winRate,
        double lossRate,
        int currentStreak,
        int maxStreak,
        int perfectPuzzles,
        Map<Integer, Integer> mistakeHistogram
) {
    public PlayerStatsData {
        mistakeHistogram = Map.copyOf(mistakeHistogram);
    }
}
