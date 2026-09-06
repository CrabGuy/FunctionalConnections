package client.json;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import shared.dto.*;

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

  public static GameInfoData gameInfoFromJson(String json) {
    JsonObject root = GSON.fromJson(json, JsonObject.class);
    if (root.has("data") && !root.get("data").isJsonNull()) {
      return GSON.fromJson(root.get("data"), GameInfoData.class);
    }
    return GSON.fromJson(root, GameInfoData.class);
  }
}
