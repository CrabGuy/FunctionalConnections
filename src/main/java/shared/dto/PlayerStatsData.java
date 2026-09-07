package shared.dto;

public record PlayerStatsData(
    int puzzlesCompleted,
    double winRate,
    double lossRate,
    int currentStreak,
    int maxStreak,
    int perfectPuzzles,
    MistakeHistogram mistakeHistogram) {}