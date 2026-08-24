package server.handlers;

import server.GameManager;
import server.ServiceContext;
import shared.ErrorCode;
import shared.Request;
import shared.Response;

import java.util.Optional;

public final class RequestGameStatisticsHandler implements RequestHandler<Request.RequestGameStatistics> {
    @Override
    public Response handle(Request.RequestGameStatistics request, ServiceContext ctx, String currentUser) {
        if (currentUser == null) {
            return Response.error(ErrorCode.USER_NOT_LOGGED_IN);
        }
        Optional<GameManager.Game> game = ctx.gameQuery().resolveGame(request.gameId());
        if (game.isEmpty()) {
            return Response.error(ErrorCode.GAME_NOT_FOUND);
        }
        GameManager.Game g = game.get();
        ctx.gameQuery().recordCompletedGameIfEnded(g, currentUser);
        return Response.success(ctx.formatter().buildGameStatistics(g));
    }
}