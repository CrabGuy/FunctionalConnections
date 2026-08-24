package server.handlers;

import server.GameManager;
import server.ServiceContext;
import server.User;
import shared.ErrorCode;
import shared.Request;
import shared.Response;

public final class RequestPersonalStatsHandler implements RequestHandler<Request.RequestPersonalStats> {
    @Override
    public Response handle(Request.RequestPersonalStats request, ServiceContext ctx, String currentUser) {
        if (currentUser == null) {
            return Response.error(ErrorCode.USER_NOT_LOGGED_IN);
        }
        User user = ctx.userManager().get(currentUser);
        if (user == null) {
            return Response.error(ErrorCode.USER_NOT_FOUND);
        }
        GameManager.Game activeGame = ctx.gameManager().getActiveGame();
        ctx.gameQuery().recordCompletedGameIfEnded(activeGame, currentUser);
        return Response.success(ctx.formatter().buildPersonalStats(user));
    }
}