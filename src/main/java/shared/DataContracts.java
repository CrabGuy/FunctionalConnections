package shared;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DataContracts {
    private DataContracts() {}

    public record SolvedGroupDto(String category, Set<String> words) {}
    public record GameGroupDto(String category, Set<String> words) {}

    public sealed interface GameStateDto permits OngoingGameStateDto, CompletedGameStateDto {
        Long gameId();
    }

    public record OngoingGameStateDto(
            Long gameId,
            String status,
            Long remainingTimeMs,
            long score,
            long mistakes,
            List<SolvedGroupDto> solvedGroups,
            List<String> remainingWords
    ) implements GameStateDto {}

    public record CompletedGameStateDto(
            Long gameId,
            String status,
            long score,
            long mistakes,
            List<SolvedGroupDto> solvedGroups,
            List<GameGroupDto> allGroups
    ) implements GameStateDto {}

    public record ProposalOutcomeDto(String status, boolean lastGuessCorrect) {}

    public record GameStatsDto(
            Long gameId,
            Long remainingTimeMs,
            long totalPlayers,
            long inProgressPlayers,
            long finishedPlayers,
            long wins,
            Double avgScore
    ) {}

    public record PlayerStatsDto(
            int puzzlesCompleted,
            double winRate,
            double lossRate,
            int currentStreak,
            int maxStreak,
            int perfectPuzzles,
            Map<Integer, Long> mistakeHistogram
    ) {}

    public record LeaderboardDto(Integer position, List<LeaderboardEntry> entries) {}

    public record LeaderboardEntry(String username, int wins) {}
}