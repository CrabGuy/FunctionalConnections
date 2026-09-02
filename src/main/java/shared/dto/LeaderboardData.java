package shared.dto;
import java.util.List;
public record LeaderboardData(
        List<LeaderboardEntry> topPlayers,
        LeaderboardEntry requestedPlayer,
        int totalPlayers
) {
    public LeaderboardData {
        topPlayers = List.copyOf(topPlayers);
    }
}
