package shared.dto;
public record GameStatsData(
        long gameId,
        boolean completed,
        long expiresAt,
        int totalParticipants,
        int activePlayers,
        int completedPlayers,
        int winners,
        double averageScore
) {
}
