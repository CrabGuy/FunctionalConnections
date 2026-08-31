package server.service;

import server.UserManager;
import server.game.GameRepository;
import server.game.GameClock;
import shared.DataContracts;
import shared.ErrorCode;
import shared.Request;
import shared.Response;

public final class AuthService {
    private final UserManager userManager;
    private final GameRepository gameRepository;
    private final GameClock clock;

    public AuthService(UserManager userManager, GameRepository gameRepository, GameClock clock) {
        this.userManager = userManager;
        this.gameRepository = gameRepository;
        this.clock = clock;
    }

    public Response<Void> register(Request.Register request) {
        if (request.username() == null || request.username().isBlank() ||
                request.psw() == null || request.psw().isBlank()) {
            return Response.error(ErrorCode.INVALID_CREDENTIALS_FORMAT);
        }
        boolean success = userManager.register(request.username(), request.psw());
        return success ? Response.success(null) : Response.error(ErrorCode.USERNAME_ALREADY_EXISTS);
    }

    public Response<DataContracts.GameStateDto> login(Request.Login request) {
        if (request.username() == null || request.psw() == null) {
            return Response.error(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!userManager.authenticate(request.username(), request.psw())) {
            return Response.error(ErrorCode.INVALID_USERNAME_OR_PASSWORD);
        }
        var game = gameRepository.getActiveGame();
        return Response.success(GameService.buildGameStateDto(gameRepository, game, request.username(), clock));
    }

    public Response<Void> logout(String currentUser) {
        return currentUser == null ? Response.error(ErrorCode.USER_NOT_LOGGED_IN) : Response.success(null);
    }

    public Response<Void> updateCredentials(Request.UpdateCredentials request, String currentUser) {
        if (request.oldUsername() == null || request.oldUsername().isBlank()) {
            return Response.error(ErrorCode.INVALID_USERNAME);
        }
        if (currentUser != null && !currentUser.equals(request.oldUsername())) {
            return Response.error(ErrorCode.UNAUTHORIZED_OR_USER_MISMATCH);
        }
        var result = userManager.updateCredentials(request.oldUsername(), request.oldPsw(),
                request.newUsername(), request.newPsw());
        return switch (result) {
            case SUCCESS -> Response.success(null);
            case INVALID_CREDENTIALS -> Response.error(ErrorCode.INVALID_CREDENTIALS);
            case TARGET_USERNAME_TAKEN -> Response.error(ErrorCode.TARGET_USERNAME_TAKEN);
        };
    }
}