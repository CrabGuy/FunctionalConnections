package client.ui;

import client.connection.ConnectionManager;
import java.io.IOException;
import shared.dto.*;

/**
 * Static utility methods that encapsulate high-level client actions such as login, registration,
 * logout, and retrieving game/statistics data.
 */
public final class ClientActions {

  private ClientActions() {
    // Prevent instantiation
  }

  /**
   * Result of a login attempt.
   *
   * @param success whether the login succeeded
   * @param accountToken the account token if successful, otherwise {@code null}
   * @param errorMessage error message if failed, otherwise {@code null}
   */
  public record LoginResult(boolean success, String accountToken, String errorMessage) {}

  /**
   * Result of a registration attempt.
   *
   * @param success whether the registration succeeded
   * @param username the registered username if successful, otherwise {@code null}
   * @param errorMessage error message if failed, otherwise {@code null}
   */
  public record RegisterResult(boolean success, String username, String errorMessage) {}

  /**
   * Result of a credential update attempt.
   *
   * @param success whether the update succeeded
   * @param newUsername the new username if successful, otherwise {@code null}
   * @param errorMessage error message if failed, otherwise {@code null}
   */
  public record UpdateCredentialsResult(boolean success, String newUsername, String errorMessage) {}

  /**
   * Attempts to log in with the given credentials.
   *
   * @param connectionManager the connection manager
   * @param username the username
   * @param password the password
   * @param udpPort the UDP port for notifications
   * @return a {@link LoginResult} containing the outcome
   * @throws IOException if an I/O error occurs
   */
  public static LoginResult login(
      ConnectionManager connectionManager, String username, String password, int udpPort)
      throws IOException {
    @SuppressWarnings("unchecked")
    ApiResponse<LoginData> response =
        (ApiResponse<LoginData>)
            connectionManager.send(new LoginRequest(username, password, udpPort));
    if (response.success()) {
      return new LoginResult(true, response.data().accountToken(), null);
    }
    return new LoginResult(false, null, response.error().message());
  }

  /**
   * Attempts to register a new account.
   *
   * @param connectionManager the connection manager
   * @param username the desired username
   * @param password the desired password
   * @return a {@link RegisterResult} containing the outcome
   * @throws IOException if an I/O error occurs
   */
  public static RegisterResult register(
      ConnectionManager connectionManager, String username, String password) throws IOException {
    @SuppressWarnings("unchecked")
    ApiResponse<RegisterData> response =
        (ApiResponse<RegisterData>) connectionManager.send(new RegisterRequest(username, password));
    if (response.success()) {
      return new RegisterResult(true, response.data().username(), null);
    }
    return new RegisterResult(false, null, response.error().message());
  }

  /**
   * Logs out the current user.
   *
   * @param connectionManager the connection manager
   * @param accountToken the account token
   * @return true if logout was successful, false otherwise
   * @throws IOException if an I/O error occurs
   */
  public static boolean logout(ConnectionManager connectionManager, String accountToken)
      throws IOException {
    @SuppressWarnings("unchecked")
    ApiResponse<LogoutData> response =
        (ApiResponse<LogoutData>) connectionManager.send(new LogoutRequest(accountToken));
    return response.success();
  }

  /**
   * Updates the user's credentials (username and/or password).
   *
   * @param connectionManager the connection manager
   * @param oldUsername the current username
   * @param newUsername the new username (or blank to keep)
   * @param oldPassword the current password
   * @param newPassword the new password (or blank to keep)
   * @return a {@link UpdateCredentialsResult} containing the outcome
   * @throws IOException if an I/O error occurs
   */
  public static UpdateCredentialsResult updateCredentials(
      ConnectionManager connectionManager,
      String oldUsername,
      String newUsername,
      String oldPassword,
      String newPassword)
      throws IOException {
    @SuppressWarnings("unchecked")
    ApiResponse<UpdateCredentialsData> response =
        (ApiResponse<UpdateCredentialsData>)
            connectionManager.send(
                new UpdateCredentialsRequest(oldUsername, newUsername, oldPassword, newPassword));
    if (response.success()) {
      return new UpdateCredentialsResult(true, response.data().newUsername(), null);
    }
    return new UpdateCredentialsResult(false, null, response.error().message());
  }

  /**
   * Fetches game information for the current or specified game.
   *
   * @param connectionManager the connection manager
   * @param accountToken the account token
   * @param gameId the game ID, or {@code null} for current game
   * @return the API response with {@link GameInfoData}
   * @throws IOException if an I/O error occurs
   */
  @SuppressWarnings("unchecked")
  public static ApiResponse<GameInfoData> fetchGameInfo(
      ConnectionManager connectionManager, String accountToken, Long gameId) throws IOException {
    return (ApiResponse<GameInfoData>)
        connectionManager.send(new RequestGameInfoRequest(accountToken, gameId));
  }

  /**
   * Fetches game statistics for the current or specified game.
   *
   * @param connectionManager the connection manager
   * @param accountToken the account token
   * @param gameId the game ID, or {@code null} for current game
   * @return the API response with {@link GameStatsData}
   * @throws IOException if an I/O error occurs
   */
  @SuppressWarnings("unchecked")
  public static ApiResponse<GameStatsData> fetchGameStats(
      ConnectionManager connectionManager, String accountToken, Long gameId) throws IOException {
    return (ApiResponse<GameStatsData>)
        connectionManager.send(new RequestGameStatsRequest(accountToken, gameId));
  }

  /**
   * Fetches the leaderboard.
   *
   * @param connectionManager the connection manager
   * @param accountToken the account token
   * @param playerName optional specific player name, or {@code null}
   * @param topPlayers optional number of top players, or {@code null} for all
   * @return the API response with {@link LeaderboardData}
   * @throws IOException if an I/O error occurs
   */
  @SuppressWarnings("unchecked")
  public static ApiResponse<LeaderboardData> fetchLeaderboard(
      ConnectionManager connectionManager,
      String accountToken,
      String playerName,
      Integer topPlayers)
      throws IOException {
    return (ApiResponse<LeaderboardData>)
        connectionManager.send(new RequestLeaderboardRequest(accountToken, playerName, topPlayers));
  }

  /**
   * Fetches statistics for the logged-in player.
   *
   * @param connectionManager the connection manager
   * @param accountToken the account token
   * @return the API response with {@link PlayerStatsData}
   * @throws IOException if an I/O error occurs
   */
  @SuppressWarnings("unchecked")
  public static ApiResponse<PlayerStatsData> fetchPlayerStats(
      ConnectionManager connectionManager, String accountToken) throws IOException {
    return (ApiResponse<PlayerStatsData>)
        connectionManager.send(new RequestPlayerStatsRequest(accountToken));
  }
}
