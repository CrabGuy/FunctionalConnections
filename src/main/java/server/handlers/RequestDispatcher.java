package server.handlers;

import server.ServiceContext;
import shared.ErrorCode;
import shared.Request;
import shared.Response;

public final class RequestDispatcher {
    private final SignupHandler signupHandler = new SignupHandler();
    private final UpdateCredentialsHandler updateCredentialsHandler = new UpdateCredentialsHandler();
    private final LoginHandler loginHandler = new LoginHandler();
    private final LogoutHandler logoutHandler = new LogoutHandler();
    private final SendAnswerHandler sendAnswerHandler = new SendAnswerHandler();
    private final RequestGameStateHandler requestGameStateHandler = new RequestGameStateHandler();
    private final RequestGameStatisticsHandler requestGameStatisticsHandler = new RequestGameStatisticsHandler();
    private final RequestLeaderboardHandler requestLeaderboardHandler = new RequestLeaderboardHandler();
    private final RequestPersonalStatsHandler requestPersonalStatsHandler = new RequestPersonalStatsHandler();

    public RequestDispatcher(ServiceContext ctx) {
    }

    public Response dispatch(Request request, ServiceContext ctx, String currentUser) {
        if (request == null || request.operation() == null) {
            return Response.error(ErrorCode.INVALID_REQUEST);
        }
        if (request instanceof Request.Signup signup) {
            return signupHandler.handle(signup, ctx, currentUser);
        }
        if (request instanceof Request.UpdateCredentials updateCredentials) {
            return updateCredentialsHandler.handle(updateCredentials, ctx, currentUser);
        }
        if (request instanceof Request.Login login) {
            return loginHandler.handle(login, ctx, currentUser);
        }
        if (request instanceof Request.Logout logout) {
            return logoutHandler.handle(logout, ctx, currentUser);
        }
        if (request instanceof Request.SendAnswer sendAnswer) {
            return sendAnswerHandler.handle(sendAnswer, ctx, currentUser);
        }
        if (request instanceof Request.RequestGameState requestGameState) {
            return requestGameStateHandler.handle(requestGameState, ctx, currentUser);
        }
        if (request instanceof Request.RequestGameStatistics requestGameStatistics) {
            return requestGameStatisticsHandler.handle(requestGameStatistics, ctx, currentUser);
        }
        if (request instanceof Request.RequestLeaderboardInfo requestLeaderboardInfo) {
            return requestLeaderboardHandler.handle(requestLeaderboardInfo, ctx, currentUser);
        }
        if (request instanceof Request.RequestPersonalStats requestPersonalStats) {
            return requestPersonalStatsHandler.handle(requestPersonalStats, ctx, currentUser);
        }
        return Response.error(ErrorCode.UNKNOWN_REQUEST);
    }
}