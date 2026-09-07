package client.json;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import shared.dto.*;

/** Provides JSON serialization and deserialization helpers for API requests and responses. */
public final class JsonCodec {

  private static final Gson GSON = new Gson();

  private JsonCodec() {
    // Prevent instantiation
  }

  /**
   * Serializes an object to JSON.
   *
   * @param obj the object to serialize
   * @return the JSON string
   */
  public static String toJson(Object obj) {
    return GSON.toJson(obj);
  }

  /**
   * Deserializes a JSON response string into a typed {@link ApiResponse} based on the operation.
   *
   * @param json the JSON string
   * @param operation the operation name (e.g., "login", "register")
   * @return the typed ApiResponse
   * @throws IllegalArgumentException if the operation is unknown
   */
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

  /**
   * Deserializes a JSON string into a {@link GameEndNotification}.
   *
   * @param json the JSON string
   * @return the game end notification
   */
  public static GameEndNotification gameEndNotificationFromJson(String json) {
    return GSON.fromJson(json, GameEndNotification.class);
  }
}
