package shared;

import java.util.List;
import java.util.Set;

public final class DataContracts {
    private DataContracts() {}

    public record GameStateDto(
        Long gameId,
        String status,
        Long remainingTimeMs,
        long score,
        long mistakes,
        List<Set<String>> solvedGroups,
        List<String> remainingWords,
        List<String> allGroups
    ) {}

    public record ProposalOutcomeDto(
        String status,
        boolean lastGuessCorrect
    ) {}

    public record GameStatsDto(
        Long gameId,
        Long remainingTimeMs,
        long totalPlayers,
        long inProgressPlayers,
        long finishedPlayers,
        long wins,
        double avgScore
    ) {}

    public record PlayerStatsDto(
        int puzzlesCompleted,
        double winRate,
        double lossRate,
        int currentStreak,
        int maxStreak,
        int perfectPuzzles,
        String mistakeHistogram
    ) {}

    public record LeaderboardDto(
        Integer position,
        List<LeaderboardEntry> entries
    ) {}

    public record LeaderboardEntry(String username, int wins) {}
}