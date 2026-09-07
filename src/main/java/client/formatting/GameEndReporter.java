package client.formatting;

import client.connection.ConnectionManager;
import java.io.IOException;
import shared.dto.ApiResponse;
import shared.dto.GameInfoData;
import shared.dto.GameStatsData;
import shared.dto.RequestGameInfoRequest;
import shared.dto.RequestGameStatsRequest;

public final class GameEndReporter {
    private GameEndReporter() {}

    public static String fetchAndFormat(ConnectionManager connectionManager, String accountToken, long gameId)
            throws IOException {
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

    public static String fetchGameStatsOnly(ConnectionManager connectionManager, String accountToken, long gameId)
            throws IOException {
        @SuppressWarnings("unchecked")
        ApiResponse<GameStatsData> statsResponse =
                (ApiResponse<GameStatsData>)
                        connectionManager.send(new RequestGameStatsRequest(accountToken, gameId));
        return statsResponse.success()
                ? OutputFormatter.formatGameStats(statsResponse.data(), System.currentTimeMillis())
                : OutputFormatter.formatError(statsResponse.error().message());
    }
}