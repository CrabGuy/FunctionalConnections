package client.json;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import shared.dto.ApiResponse;
import shared.dto.GameEndNotification;
import shared.dto.GameInfoData;
import shared.dto.GameStatsData;
import shared.dto.LeaderboardData;
import shared.dto.LoginData;
import shared.dto.LogoutData;
import shared.dto.PlayerStatsData;
import shared.dto.RegisterData;
import shared.dto.UpdateCredentialsData;

public final class JsonCodec {
  private static final Gson GSON = new Gson();

  private JsonCodec() {}

  public static String toJson(Object obj) {
    return GSON.toJson(obj);
  }

  public static ApiResponse<?> fromJson(String json, String operation) {
    Type type =
        switch (operation) {
          case "register" -> new TypeToken<ApiResponse<RegisterData>>() {}.getType();
          case "login" -> new TypeToken<ApiResponse<LoginData>>() {}.getType();
          case "logout" -> new TypeToken<ApiResponse<LogoutData>>() {}.getType();
          case "updateCredentials" ->
              new TypeToken<ApiResponse<UpdateCredentialsData>>() {}.getType();
          case "submitProposal" -> new TypeToken<ApiResponse<GameInfoData>>() {}.getType();
          case "requestGameInfo" -> new TypeToken<ApiResponse<GameInfoData>>() {}.getType();
          case "requestGameStats" -> new TypeToken<ApiResponse<GameStatsData>>() {}.getType();
          case "requestLeaderboard" -> new TypeToken<ApiResponse<LeaderboardData>>() {}.getType();
          case "requestPlayerStats" -> new TypeToken<ApiResponse<PlayerStatsData>>() {}.getType();
          default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        };
    return GSON.fromJson(json, type);
  }

  public static GameEndNotification gameEndNotificationFromJson(String json) {
    return GSON.fromJson(json, GameEndNotification.class);
  }
}
