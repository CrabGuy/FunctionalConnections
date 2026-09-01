package server.service;

import server.UserManager;
import server.game.PlayerStatsCalculator;
import shared.DataContracts;
import shared.ErrorCode;
import shared.Request;
import shared.Response;

import java.util.Comparator;
import java.util.List;

public final class StatsService {
    private final UserManager userManager;
    private final PlayerStatsCalculator statsCalculator;

    public StatsService(UserManager userManager, PlayerStatsCalculator statsCalculator) {
        this.userManager = userManager;
        this.statsCalculator = statsCalculator;
    }

    public Response<DataContracts.PlayerStatsDto> getPlayerStats(String currentUser) {
        if (currentUser == null) return Response.error(ErrorCode.USER_NOT_LOGGED_IN);
        if (!userManager.usernameExists(currentUser)) return Response.error(ErrorCode.USER_NOT_FOUND);
        return Response.success(statsCalculator.calculateStats(currentUser));
    }

    public Response<DataContracts.LeaderboardDto> getLeaderboard(String currentUser, Request.RequestLeaderboard request) {
        if (currentUser == null) return Response.error(ErrorCode.USER_NOT_LOGGED_IN);

        List<DataContracts.LeaderboardEntry> sortedEntries = userManager.getAllUsernames().stream()
                .map(u -> new DataContracts.LeaderboardEntry(u, statsCalculator.calculateWins(u)))
                .sorted(Comparator.comparingInt(DataContracts.LeaderboardEntry::wins).reversed()
                        .thenComparing(DataContracts.LeaderboardEntry::username))
                .toList();

        if (request.playerName() != null && !request.playerName().isBlank()) {
            String targetPlayer = request.playerName();
            if (!userManager.usernameExists(targetPlayer)) {
                return Response.error(ErrorCode.PLAYER_NOT_FOUND);
            }
            int position = 1;
            for (DataContracts.LeaderboardEntry entry : sortedEntries) {
                if (entry.username().equals(targetPlayer)) {
                    break;
                }
                position++;
            }
            return Response.success(new DataContracts.LeaderboardDto(position, List.of()));
        }

        int limit = request.topPlayers() == null || request.topPlayers() <= 0 ? Integer.MAX_VALUE : request.topPlayers();
        List<DataContracts.LeaderboardEntry> limitedEntries = sortedEntries.stream()
                .limit(limit)
                .toList();
        return Response.success(new DataContracts.LeaderboardDto(null, limitedEntries));
    }
}