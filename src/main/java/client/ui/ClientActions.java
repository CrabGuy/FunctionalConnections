package client.ui;

import client.connection.ConnectionManager;
import java.io.IOException;
import shared.dto.*;

public final class ClientActions {
    private ClientActions() {}

    public record LoginResult(boolean success, String accountToken, String errorMessage) {}

    public record RegisterResult(boolean success, String username, String errorMessage) {}

    public record UpdateCredentialsResult(boolean success, String newUsername, String errorMessage) {}

    public static LoginResult login(
            ConnectionManager connectionManager,
            String username,
            String password,
            int udpPort) throws IOException {
        @SuppressWarnings("unchecked")
        ApiResponse<LoginData> response =
                (ApiResponse<LoginData>)
                        connectionManager.send(new LoginRequest(username, password, udpPort));
        if (response.success()) {
            return new LoginResult(true, response.data().accountToken(), null);
        }
        return new LoginResult(false, null, response.error().message());
    }

    public static RegisterResult register(
            ConnectionManager connectionManager, String username, String password)
            throws IOException {
        @SuppressWarnings("unchecked")
        ApiResponse<RegisterData> response =
                (ApiResponse<RegisterData>)
                        connectionManager.send(new RegisterRequest(username, password));
        if (response.success()) {
            return new RegisterResult(true, response.data().username(), null);
        }
        return new RegisterResult(false, null, response.error().message());
    }

    public static boolean logout(ConnectionManager connectionManager, String accountToken)
            throws IOException {
        @SuppressWarnings("unchecked")
        ApiResponse<LogoutData> response =
                (ApiResponse<LogoutData>) connectionManager.send(new LogoutRequest(accountToken));
        return response.success();
    }

    public static UpdateCredentialsResult updateCredentials(
            ConnectionManager connectionManager,
            String oldUsername,
            String newUsername,
            String oldPassword,
            String newPassword) throws IOException {
        @SuppressWarnings("unchecked")
        ApiResponse<UpdateCredentialsData> response =
                (ApiResponse<UpdateCredentialsData>)
                        connectionManager.send(
                                new UpdateCredentialsRequest(
                                        oldUsername, newUsername, oldPassword, newPassword));
        if (response.success()) {
            return new UpdateCredentialsResult(true, response.data().newUsername(), null);
        }
        return new UpdateCredentialsResult(false, null, response.error().message());
    }

    @SuppressWarnings("unchecked")
    public static ApiResponse<GameInfoData> fetchGameInfo(
            ConnectionManager connectionManager, String accountToken, Long gameId)
            throws IOException {
        return (ApiResponse<GameInfoData>)
                connectionManager.send(new RequestGameInfoRequest(accountToken, gameId));
    }

    @SuppressWarnings("unchecked")
    public static ApiResponse<GameStatsData> fetchGameStats(
            ConnectionManager connectionManager, String accountToken, Long gameId)
            throws IOException {
        return (ApiResponse<GameStatsData>)
                connectionManager.send(new RequestGameStatsRequest(accountToken, gameId));
    }

    @SuppressWarnings("unchecked")
    public static ApiResponse<LeaderboardData> fetchLeaderboard(
            ConnectionManager connectionManager,
            String accountToken,
            String playerName,
            Integer topPlayers) throws IOException {
        return (ApiResponse<LeaderboardData>)
                connectionManager.send(
                        new RequestLeaderboardRequest(accountToken, playerName, topPlayers));
    }

    @SuppressWarnings("unchecked")
    public static ApiResponse<PlayerStatsData> fetchPlayerStats(
            ConnectionManager connectionManager, String accountToken) throws IOException {
        return (ApiResponse<PlayerStatsData>)
                connectionManager.send(new RequestPlayerStatsRequest(accountToken));
    }
}