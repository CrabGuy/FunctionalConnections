package server.service;

import server.UserManager;
import server.User;
import shared.DataContracts;
import shared.ErrorCode;
import shared.Request;
import shared.Response;

import java.util.List;
import java.util.stream.Collectors;

public final class StatsService {
    private final UserManager userManager;

    public StatsService(UserManager userManager) {
        this.userManager = userManager;
    }

    public Response<DataContracts.PlayerStatsDto> getPlayerStats(String currentUser) {
        if (currentUser == null) return Response.error(ErrorCode.USER_NOT_LOGGED_IN);
        User user = userManager.get(currentUser);
        if (user == null) return Response.error(ErrorCode.USER_NOT_FOUND);
        int completed = user.games().size();
        int wins = user.getWins();
        double winRate = completed == 0 ? 0 : wins * 100.0 / completed;
        double lossRate = completed == 0 ? 0 : (completed - wins) * 100.0 / completed;
        String hist = user.getMistakeHistogram().entrySet().stream()
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining(","));
        return Response.success(new DataContracts.PlayerStatsDto(
                completed, winRate, lossRate, user.currentStreak(),
                user.maxStreak(), user.getPerfectPuzzles(), hist.isEmpty() ? "NONE" : hist
        ));
    }

    public Response<DataContracts.LeaderboardDto> getLeaderboard(String currentUser, Request.RequestLeaderboard request) {
        if (currentUser == null) return Response.error(ErrorCode.USER_NOT_LOGGED_IN);
        if (request.playerName() != null && !request.playerName().isBlank()) {
            if (!userManager.usernameExists(request.playerName())) {
                return Response.error(ErrorCode.PLAYER_NOT_FOUND);
            }
            return Response.success(new DataContracts.LeaderboardDto(userManager.getPosition(request.playerName()), List.of()));
        }
        int limit = request.topPlayers() == null || request.topPlayers() <= 0 ? Integer.MAX_VALUE : request.topPlayers();
        List<DataContracts.LeaderboardEntry> entries = userManager.getLeaderboard()
                .limit(limit)
                .map(u -> new DataContracts.LeaderboardEntry(u.username(), u.getWins()))
                .toList();
        return Response.success(new DataContracts.LeaderboardDto(null, entries));
    }
}