package shared.dto;
import java.util.Map;
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
