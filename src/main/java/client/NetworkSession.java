package client;

import shared.DataContracts;
import shared.Request;
import shared.Response;

import java.io.IOException;
import java.util.List;

public final class NetworkSession implements AutoCloseable {
    private final NetworkClient networkClient;
    private String currentUser;
    private Long currentGameId;

    public NetworkSession(String host, int port) throws IOException {
        this.networkClient = new NetworkClient(host, port);
    }

    public Response<Void> register(String username, String password) {
        return send(new Request.Register(username, password), Void.class);
    }

    public Response<DataContracts.GameStateDto> login(String username, String password) {
        Response<DataContracts.GameStateDto> response = send(new Request.Login(username, password), DataContracts.GameStateDto.class);
        if (response.success()) {
            currentUser = username;
            if (response.result() != null) {
                currentGameId = response.result().gameId();
            }
        }
        return response;
    }

    public Response<Void> logout() {
        Response<Void> response = send(new Request.Logout(), Void.class);
        if (response.success()) {
            currentUser = null;
            currentGameId = null;
        }
        return response;
    }

    public Response<Void> updateCredentials(String oldUsername, String oldPassword,
                                            String newUsername, String newPassword) {
        Response<Void> response = send(new Request.UpdateCredentials(oldUsername, oldPassword, newUsername, newPassword), Void.class);
        if (response.success() && currentUser != null && currentUser.equals(oldUsername) &&
                newUsername != null && !newUsername.isBlank() && !newUsername.equals(oldUsername)) {
            currentUser = newUsername;
        }
        return response;
    }

    public Response<DataContracts.ProposalOutcomeDto> submitProposal(List<String> words) {
        return send(new Request.SubmitProposal(words), DataContracts.ProposalOutcomeDto.class);
    }

    public Response<DataContracts.GameStateDto> getGameInfo(Long gameId) {
        return send(new Request.RequestGameInfo(gameId), DataContracts.GameStateDto.class);
    }

    public Response<DataContracts.GameStatsDto> getGameStats(Long gameId) {
        return send(new Request.RequestGameStats(gameId), DataContracts.GameStatsDto.class);
    }

    public Response<DataContracts.PlayerStatsDto> getPlayerStats() {
        return send(new Request.RequestPlayerStats(), DataContracts.PlayerStatsDto.class);
    }

    public Response<DataContracts.LeaderboardDto> getLeaderboard(String playerName, Integer topPlayers) {
        return send(new Request.RequestLeaderboard(playerName, topPlayers), DataContracts.LeaderboardDto.class);
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public Long getCurrentGameId() {
        return currentGameId;
    }

    private <T> Response<T> send(Request request, Class<T> responseType) {
        try {
            return networkClient.sendRequest(request, responseType);
        } catch (IOException e) {
            return Response.error("Communication failure: " + e.getMessage());
        }
    }

    @Override
    public void close() throws IOException {
        networkClient.close();
    }
}