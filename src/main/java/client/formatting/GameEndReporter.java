package client.formatting;

import client.connection.ConnectionManager;
import java.io.IOException;
import shared.dto.*;

/**
 * Utility class for fetching and formatting game information and statistics at the end of a game.
 */
public final class GameEndReporter {

  private GameEndReporter() {
    // Prevent instantiation
  }

  /**
   * Fetches both game info and stats for the given game and returns a formatted combined report.
   *
   * @param connectionManager the connection manager used to send requests
   * @param accountToken the account token of the logged-in user
   * @param gameId the ID of the game
   * @return a formatted string containing game info and stats
   * @throws IOException if an I/O error occurs during requests
   */
  public static String fetchAndFormat(
      ConnectionManager connectionManager, String accountToken, long gameId) throws IOException {
    @SuppressWarnings("unchecked")
    ApiResponse<GameInfoData> infoResponse =
        (ApiResponse<GameInfoData>)
            connectionManager.send(new RequestGameInfoRequest(accountToken, gameId));

    @SuppressWarnings("unchecked")
    ApiResponse<GameStatsData> statsResponse =
        (ApiResponse<GameStatsData>)
            connectionManager.send(new RequestGameStatsRequest(accountToken, gameId));

    StringBuilder sb = new StringBuilder();
    sb.append(
        infoResponse.success()
            ? OutputFormatter.formatGameInfo(infoResponse.data(), System.currentTimeMillis())
            : OutputFormatter.formatError(infoResponse.error().message()));
    sb.append("\n\n");
    sb.append(
        statsResponse.success()
            ? OutputFormatter.formatGameStats(statsResponse.data(), System.currentTimeMillis())
            : OutputFormatter.formatError(statsResponse.error().message()));
    return sb.toString();
  }

  /**
   * Fetches only game statistics for the given game and returns a formatted string.
   *
   * @param connectionManager the connection manager used to send requests
   * @param accountToken the account token of the logged-in user
   * @param gameId the ID of the game
   * @return a formatted string containing game statistics
   * @throws IOException if an I/O error occurs during the request
   */
  public static String fetchGameStatsOnly(
      ConnectionManager connectionManager, String accountToken, long gameId) throws IOException {
    @SuppressWarnings("unchecked")
    ApiResponse<GameStatsData> statsResponse =
        (ApiResponse<GameStatsData>)
            connectionManager.send(new RequestGameStatsRequest(accountToken, gameId));

    return statsResponse.success()
        ? OutputFormatter.formatGameStats(statsResponse.data(), System.currentTimeMillis())
        : OutputFormatter.formatError(statsResponse.error().message());
  }
}
