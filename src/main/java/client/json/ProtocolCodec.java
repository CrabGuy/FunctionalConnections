package client.json;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import shared.dto.*;

import java.util.*;

public final class ProtocolCodec {

    private static final Gson gson = new Gson();

    private ProtocolCodec() {}

    // ------------------------------------------------------------------
    // Request serialization (unchanged – no unchecked casts here)
    // ------------------------------------------------------------------
    public static String requestToJson(ApiRequest request) {
        Map<String, Object> object = new LinkedHashMap<>();
        object.put("operation", request.getOperation());
        if (request instanceof RegisterRequest r) {
            object.put("username", r.username());
            object.put("psw", r.password());
        } else if (request instanceof UpdateCredentialsRequest r) {
            object.put("oldUsername", r.oldUsername());
            object.put("newUsername", r.newUsername());
            object.put("oldPsw", r.oldPassword());
            object.put("newPsw", r.newPassword());
        } else if (request instanceof LoginRequest r) {
            object.put("username", r.username());
            object.put("psw", r.password());
            object.put("udpPort", r.udpPort());
        } else if (request instanceof LogoutRequest r) {
            object.put("accountToken", r.accountToken());
        } else if (request instanceof SubmitProposalRequest r) {
            object.put("accountToken", r.accountToken());
            object.put("words", r.words());
        } else if (request instanceof RequestGameInfoRequest r) {
            object.put("accountToken", r.accountToken());
            if (r.gameId() != null) object.put("gameId", r.gameId());
        } else if (request instanceof RequestGameStatsRequest r) {
            object.put("accountToken", r.accountToken());
            if (r.gameId() != null) object.put("gameId", r.gameId());
        } else if (request instanceof RequestLeaderboardRequest r) {
            object.put("accountToken", r.accountToken());
            if (r.playerName() != null && !r.playerName().isBlank())
                object.put("playerName", r.playerName());
            if (r.topPlayers() != null) object.put("topPlayers", r.topPlayers());
        } else if (request instanceof RequestPlayerStatsRequest r) {
            object.put("accountToken", r.accountToken());
        } else {
            throw new IllegalArgumentException("Unsupported request type: " + request.getClass());
        }
        return gson.toJson(object);
    }

    // ------------------------------------------------------------------
    // Request deserialization – now uses JsonObject, no unchecked casts
    // ------------------------------------------------------------------
    public static ApiRequest requestFromJson(String json) {
        JsonObject object = gson.fromJson(json, JsonObject.class);
        String operation = object.get("operation").getAsString();

        return switch (operation) {
            case "register" -> new RegisterRequest(
                    object.get("username").getAsString(),
                    object.get("psw").getAsString()
            );
            case "updateCredentials" -> new UpdateCredentialsRequest(
                    object.get("oldUsername").getAsString(),
                    object.get("newUsername").getAsString(),
                    object.get("oldPsw").getAsString(),
                    object.get("newPsw").getAsString()
            );
            case "login" -> new LoginRequest(
                    object.get("username").getAsString(),
                    object.get("psw").getAsString(),
                    object.has("udpPort") ? object.get("udpPort").getAsInt() : 0
            );
            case "logout" -> new LogoutRequest(object.get("accountToken").getAsString());
            case "submitProposal" -> new SubmitProposalRequest(
                    object.get("accountToken").getAsString(),
                    stringList(object.get("words"))
            );
            case "requestGameInfo" -> new RequestGameInfoRequest(
                    object.get("accountToken").getAsString(),
                    nullableLong(object.get("gameId"))
            );
            case "requestGameStats" -> new RequestGameStatsRequest(
                    object.get("accountToken").getAsString(),
                    nullableLong(object.get("gameId"))
            );
            case "requestLeaderboard" -> new RequestLeaderboardRequest(
                    object.get("accountToken").getAsString(),
                    nullableString(object.get("playerName")),
                    nullableInteger(object.get("topPlayers"))
            );
            case "requestPlayerStats" -> new RequestPlayerStatsRequest(object.get("accountToken").getAsString());
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        };
    }

    // ------------------------------------------------------------------
    // Response deserialization – uses JsonObject
    // ------------------------------------------------------------------
    public static ApiResponse<?> responseFromJson(String json, String expectedOperation) {
        JsonObject object = gson.fromJson(json, JsonObject.class);
        boolean success = object.get("success").getAsBoolean();

        ApiError error = null;
        if (object.has("error") && !object.get("error").isJsonNull()) {
            error = error(object.getAsJsonObject("error"));
        }

        // Only decode data if success is true AND data exists and is not null
        Object data = null;
        if (success) {
            JsonElement rawData = object.get("data");
            if (rawData != null && !rawData.isJsonNull()) {
                data = decodeData(expectedOperation, rawData);
            } else {
                // Success but no data – this shouldn't happen, but handle gracefully
                data = null;
            }
        } else {
            // Failure: data should be null, but we might still want to include the raw data if present
            JsonElement rawData = object.get("data");
            data = (rawData != null && !rawData.isJsonNull()) ? rawData.getAsJsonObject() : null;
        }

        return new ApiResponse<>(success, error, data);
    }

    public static GameInfoData gameInfoFromJson(String json) {
        JsonObject root = gson.fromJson(json, JsonObject.class);
        if (root.has("data") && !root.get("data").isJsonNull()) {
            return gameInfo(root.getAsJsonObject("data"));
        }
        return gameInfo(root);
    }

    // ------------------------------------------------------------------
    // Private helpers – all accept/return JsonElement/JsonObject
    // ------------------------------------------------------------------
    private static Object decodeData(String operation, JsonElement rawData) {
        if (rawData == null || rawData.isJsonNull()) {
            return null;
        }
        JsonObject data = rawData.getAsJsonObject();

        return switch (operation) {
            case "register" -> new RegisterData(data.get("username").getAsString());
            case "login" -> new LoginData(data.get("accountToken").getAsString());
            case "logout" -> new LogoutData();
            case "updateCredentials" -> new UpdateCredentialsData(data.get("newUsername").getAsString());
            case "submitProposal", "requestGameInfo" -> gameInfo(data);
            case "requestGameStats" -> gameStats(data);
            case "requestLeaderboard" -> leaderboard(data);
            case "requestPlayerStats" -> playerStats(data);
            default -> rawData;  // fallback: return the raw JsonElement
        };
    }

    private static ApiError error(JsonObject data) {
        String code = data.get("code").getAsString();
        ErrorCode errorCode = ErrorCode.valueOf(code);
        return new ApiError(errorCode, data.get("message").getAsString());
    }

    private static GameInfoData gameInfo(JsonObject data) {
        List<String> words = stringList(data.get("words"));
        List<Set<String>> correctGuesses = setList(data.get("correctGuesses"));
        List<Set<String>> wrongGuesses = setList(data.get("wrongGuesses"));

        List<List<String>> correctGroups = null;
        if (data.has("correctGroups") && !data.get("correctGroups").isJsonNull()) {
            correctGroups = nestedStringList(data.get("correctGroups"));
        }

        return new GameInfoData(
                data.get("gameId").getAsLong(),
                data.get("expiresAt").getAsLong(),
                words,
                correctGuesses,
                wrongGuesses,
                correctGroups
        );
    }

    private static GameStatsData gameStats(JsonObject data) {
        return new GameStatsData(
                data.get("gameId").getAsLong(),
                data.get("completed").getAsBoolean(),
                data.get("expiresAt").getAsLong(),
                data.has("totalParticipants") ? data.get("totalParticipants").getAsInt() : 0,
                data.has("activePlayers") ? data.get("activePlayers").getAsInt() : 0,
                data.has("completedPlayers") ? data.get("completedPlayers").getAsInt() : 0,
                data.has("winners") ? data.get("winners").getAsInt() : 0,
                data.has("averageScore") ? data.get("averageScore").getAsDouble() : 0.0
        );
    }

    private static LeaderboardData leaderboard(JsonObject data) {
        List<LeaderboardEntry> topPlayers = leaderboardEntries(data.get("topPlayers"));

        LeaderboardEntry requestedPlayer = null;
        if (data.has("requestedPlayer") && !data.get("requestedPlayer").isJsonNull()) {
            requestedPlayer = leaderboardEntry(data.getAsJsonObject("requestedPlayer"));
        }

        int totalPlayers = data.has("totalPlayers") ? data.get("totalPlayers").getAsInt() : 0;
        return new LeaderboardData(topPlayers, requestedPlayer, totalPlayers);
    }

    private static PlayerStatsData playerStats(JsonObject data) {
        Map<Integer, Integer> histogram = new LinkedHashMap<>();
        if (data.has("mistakeHistogram") && !data.get("mistakeHistogram").isJsonNull()) {
            JsonObject raw = data.getAsJsonObject("mistakeHistogram");
            for (Map.Entry<String, JsonElement> entry : raw.entrySet()) {
                histogram.put(
                        Integer.parseInt(entry.getKey()),
                        entry.getValue().getAsInt()
                );
            }
        }

        return new PlayerStatsData(
                data.has("puzzlesCompleted") ? data.get("puzzlesCompleted").getAsInt() : 0,
                data.has("winRate") ? data.get("winRate").getAsDouble() : 0.0,
                data.has("lossRate") ? data.get("lossRate").getAsDouble() : 0.0,
                data.has("currentStreak") ? data.get("currentStreak").getAsInt() : 0,
                data.has("maxStreak") ? data.get("maxStreak").getAsInt() : 0,
                data.has("perfectPuzzles") ? data.get("perfectPuzzles").getAsInt() : 0,
                histogram
        );
    }

    private static List<LeaderboardEntry> leaderboardEntries(JsonElement value) {
        List<LeaderboardEntry> result = new ArrayList<>();
        if (value == null || value.isJsonNull()) {
            return result;
        }
        JsonArray array = value.getAsJsonArray();
        for (JsonElement entry : array) {
            result.add(leaderboardEntry(entry.getAsJsonObject()));
        }
        return result;
    }

    private static LeaderboardEntry leaderboardEntry(JsonObject data) {
        return new LeaderboardEntry(
                data.get("username").getAsString(),
                data.has("score") ? data.get("score").getAsInt() : 0,
                data.has("rank") ? data.get("rank").getAsInt() : 0
        );
    }

    private static List<Set<String>> setList(JsonElement value) {
        List<Set<String>> result = new ArrayList<>();
        if (value == null || value.isJsonNull()) {
            return result;
        }
        JsonArray array = value.getAsJsonArray();
        for (JsonElement item : array) {
            result.add(new LinkedHashSet<>(stringList(item)));
        }
        return result;
    }

    private static List<List<String>> nestedStringList(JsonElement value) {
        List<List<String>> result = new ArrayList<>();
        if (value == null || value.isJsonNull()) {
            return result;
        }
        JsonArray array = value.getAsJsonArray();
        for (JsonElement item : array) {
            result.add(stringList(item));
        }
        return result;
    }

    private static List<String> stringList(JsonElement value) {
        List<String> result = new ArrayList<>();
        if (value == null || value.isJsonNull()) {
            return result;
        }
        JsonArray array = value.getAsJsonArray();
        for (JsonElement item : array) {
            result.add(item.getAsString());
        }
        return result;
    }

    private static String nullableString(JsonElement value) {
        return (value == null || value.isJsonNull()) ? null : value.getAsString();
    }

    private static Integer nullableInteger(JsonElement value) {
        return (value == null || value.isJsonNull()) ? null : value.getAsInt();
    }

    private static Long nullableLong(JsonElement value) {
        return (value == null || value.isJsonNull()) ? null : value.getAsLong();
    }
}