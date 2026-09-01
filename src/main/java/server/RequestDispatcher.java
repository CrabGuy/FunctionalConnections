package server;

import server.service.AuthService;
import server.service.GameService;
import server.service.StatsService;
import shared.ErrorCode;
import shared.Request;
import shared.Response;

public final class RequestDispatcher {
    private final AuthService authService;
    private final GameService gameService;
    private final StatsService statsService;

    public RequestDispatcher(AuthService authService, GameService gameService, StatsService statsService) {
        this.authService = authService;
        this.gameService = gameService;
        this.statsService = statsService;
    }

    public Response<?> dispatch(Request request, String currentUser) {
        if (request == null) {
            return Response.error(ErrorCode.INVALID_REQUEST);
        }
        if (currentUser != null) {
            gameService.ensureAutoParticipation(currentUser);
        }
        return switch (request) {
            case Request.Register r -> authService.register(r);
            case Request.Login r -> authService.login(r);
            case Request.Logout r -> authService.logout(currentUser);
            case Request.UpdateCredentials r -> authService.updateCredentials(r, currentUser);
            case Request.SubmitProposal r -> gameService.submitProposal(currentUser, r);
            case Request.RequestGameInfo r -> gameService.getGameState(currentUser, r.gameId());
            case Request.RequestGameStats r -> gameService.getGameStats(currentUser, r.gameId());
            case Request.RequestLeaderboard r -> statsService.getLeaderboard(currentUser, r);
            case Request.RequestPlayerStats r -> statsService.getPlayerStats(currentUser);
            default -> Response.error(ErrorCode.UNKNOWN_REQUEST);
        };
    }
}