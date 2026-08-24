package server.handlers;

import server.ServiceContext;
import shared.ErrorCode;
import shared.Request;
import shared.Response;

import java.util.stream.Collectors;

public final class RequestLeaderboardHandler implements RequestHandler<Request.RequestLeaderboardInfo> {
    @Override
    public Response handle(Request.RequestLeaderboardInfo leaderboard, ServiceContext ctx, String currentUser) {
        if (currentUser == null) {
            return Response.error(ErrorCode.USER_NOT_LOGGED_IN);
        }
        if (leaderboard.playerName() != null && !leaderboard.playerName().isBlank()) {
            if (!ctx.userManager().usernameExists(leaderboard.playerName())) {
                return Response.error(ErrorCode.PLAYER_NOT_FOUND);
            }
            return Response.success("POSITION:" + ctx.userManager().getPosition(leaderboard.playerName()));
        }
        int limit = leaderboard.topPlayers() == null || leaderboard.topPlayers() <= 0
                ? Integer.MAX_VALUE
                : leaderboard.topPlayers();
        String data = ctx.userManager().getLeaderboard()
                .limit(limit)
                .map(user -> user.username() + ":" + user.getWins())
                .collect(Collectors.joining(","));
        return Response.success(data);
    }
}