package server.handlers;

import server.GameManager;
import server.ServiceContext;
import shared.ErrorCode;
import shared.Request;
import shared.Response;

public final class LoginHandler implements RequestHandler {
    @Override
    public String operation() {
        return "login";
    }

    @Override
    public Response handle(Request request, ServiceContext ctx, String currentUser) {
        if (!(request instanceof Request.Login login)) {
            return Response.error(ErrorCode.UNKNOWN_REQUEST);
        }

        if (login.username() == null || login.psw() == null) {
            return Response.error(ErrorCode.INVALID_CREDENTIALS);
        }

        if (!ctx.userManager().authenticate(login.username(), login.psw())) {
            return Response.error(ErrorCode.INVALID_USERNAME_OR_PASSWORD);
        }

        GameManager.Game game = ctx.gameManager().getActiveGame();
        ctx.gameQuery().recordCompletedGameIfEnded(game, login.username());
        GameManager.Status status = ctx.gameManager().getPlayerStatus(game, login.username());

        return Response.success("Login successful\n" + ctx.formatter().buildGameState(game, login.username(), status));
    }
}