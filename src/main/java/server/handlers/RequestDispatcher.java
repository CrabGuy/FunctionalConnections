package server.handlers;

import server.ServiceContext;
import shared.ErrorCode;
import shared.Request;
import shared.Response;

import java.util.HashMap;
import java.util.Map;

public final class RequestDispatcher {
    private final Map<String, RequestHandler> handlers = new HashMap<>();

    public RequestDispatcher(ServiceContext ctx) {
        handlers.put("signup", new SignupHandler());
        handlers.put("updateCredentials", new UpdateCredentialsHandler());
        handlers.put("login", new LoginHandler());
        handlers.put("logout", new LogoutHandler());
        handlers.put("sendAnswer", new SendAnswerHandler());
        handlers.put("requestGameState", new RequestGameStateHandler());
        handlers.put("requestGameStatistics", new RequestGameStatisticsHandler());
        handlers.put("requestLeaderboardInfo", new RequestLeaderboardHandler());
        handlers.put("requestPersonalStats", new RequestPersonalStatsHandler());
    }

    public Response dispatch(Request request, ServiceContext ctx, String currentUser) {
        if (request == null || request.operation() == null) {
            return Response.error(ErrorCode.INVALID_REQUEST);
        }

        RequestHandler handler = handlers.get(request.operation());
        if (handler == null) {
            return Response.error(ErrorCode.UNKNOWN_REQUEST);
        }

        return handler.handle(request, ctx, currentUser);
    }
}