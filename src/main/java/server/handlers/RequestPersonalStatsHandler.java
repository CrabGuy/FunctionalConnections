package server.handlers;

import server.GameManager;
import server.ServiceContext;
import server.User;
import shared.ErrorCode;
import shared.Request;
import shared.Response;

public final class RequestPersonalStatsHandler implements RequestHandler {
    @Override
    public String operation() {
        return "requestPersonalStats";
    }

    @Override
    public Response handle(Request request, ServiceContext ctx, String currentUser) {
        if (!(request instanceof Request.RequestPersonalStats)) {
            return Response.error(ErrorCode.UNKNOWN_REQUEST);
        }

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