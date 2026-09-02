package client.json;

import shared.dto.ApiError;
import shared.dto.ApiRequest;
import shared.dto.ApiResponse;
import shared.dto.GameInfoData;
import shared.dto.GameStatsData;
import shared.dto.LeaderboardData;
import shared.dto.LeaderboardEntry;
import shared.dto.LoginData;
import shared.dto.LoginRequest;
import shared.dto.LogoutData;
import shared.dto.LogoutRequest;
import shared.dto.PlayerStatsData;
import shared.dto.RegisterData;
import shared.dto.RegisterRequest;
import shared.dto.RequestGameInfoRequest;
import shared.dto.RequestGameStatsRequest;
import shared.dto.RequestLeaderboardRequest;
import shared.dto.RequestPlayerStatsRequest;
import shared.dto.SubmitProposalRequest;
import shared.dto.UpdateCredentialsData;
import shared.dto.UpdateCredentialsRequest;
import shared.dto.ErrorCode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Converts the shared protocol records to and from the required JSON wire format. */
public final class ProtocolCodec {
    private ProtocolCodec() {
    }

    public static String requestToJson(ApiRequest request) {
        Map<String, Object> object = new LinkedHashMap<>();
        object.put("operation", request.getOperation());
        if (request instanceof RegisterRequest value) {
            object.put("username", value.username());
            object.put("psw", value.password());
        } else if (request instanceof UpdateCredentialsRequest value) {
            object.put("oldUsername", value.oldUsername());
            object.put("newUsername", value.newUsername());
            object.put("oldPsw", value.oldPassword());
            object.put("newPsw", value.newPassword());
        } else if (request instanceof LoginRequest value) {
            object.put("username", value.username());
            object.put("psw", value.password());
            object.put("udpPort", value.udpPort());
        } else if (request instanceof LogoutRequest value) {
            object.put("accountToken", value.accountToken());
        } else if (request instanceof SubmitProposalRequest value) {
            object.put("accountToken", value.accountToken());
            object.put("words", value.words());
        } else if (request instanceof RequestGameInfoRequest value) {
            object.put("accountToken", value.accountToken());
            if (value.gameId() != null) {
                object.put("gameId", value.gameId());
            }
        } else if (request instanceof RequestGameStatsRequest value) {
            object.put("accountToken", value.accountToken());
            if (value.gameId() != null) {
                object.put("gameId", value.gameId());
            }
        } else if (request instanceof RequestLeaderboardRequest value) {
            object.put("accountToken", value.accountToken());
            if (value.playerName() != null && !value.playerName().isBlank()) {
                object.put("playerName", value.playerName());
            }
            if (value.topPlayers() != null) {
                object.put("topPlayers", value.topPlayers());
            }
        } else if (request instanceof RequestPlayerStatsRequest value) {
            object.put("accountToken", value.accountToken());
        } else {
            throw new IllegalArgumentException("Unsupported request type: " + request.getClass());
        }
        return JsonWriter.write(object);
    }

    public static ApiRequest requestFromJson(String json) {
        Map<String, Object> object = object(JsonParser.parse(json), "request");
        String operation = string(object, "operation");
        return switch (operation) {
            case "register" -> new RegisterRequest(string(object, "username"), string(object, "psw"));
            case "updateCredentials" -> new UpdateCredentialsRequest(
                    string(object, "oldUsername"), string(object, "newUsername"),
                    string(object, "oldPsw"), string(object, "newPsw"));
            case "login" -> new LoginRequest(
                    string(object, "username"), string(object, "psw"), integer(object, "udpPort", 0));
            case "logout" -> new LogoutRequest(string(object, "accountToken"));
            case "submitProposal" -> new SubmitProposalRequest(
                    string(object, "accountToken"), stringList(object.get("words")));
            case "requestGameInfo" -> new RequestGameInfoRequest(
                    string(object, "accountToken"), nullableLong(object.get("gameId")));
            case "requestGameStats" -> new RequestGameStatsRequest(
                    string(object, "accountToken"), nullableLong(object.get("gameId")));
            case "requestLeaderboard" -> new RequestLeaderboardRequest(
                    string(object, "accountToken"), nullableString(object.get("playerName")),
                    nullableInteger(object.get("topPlayers")));
            case "requestPlayerStats" -> new RequestPlayerStatsRequest(string(object, "accountToken"));
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        };
    }

    public static ApiResponse<?> responseFromJson(String json, String expectedOperation) {
        Map<String, Object> object = object(JsonParser.parse(json), "response");
        boolean success = bool(object, "success");
        ApiError error = object.get("error") == null ? null : error(object(object.get("error"), "error"));
        Object rawData = object.get("data");
        Object data = success ? decodeData(expectedOperation, rawData) : rawData;
        return new ApiResponse<>(success, error, data);
    }

    public static GameInfoData gameInfoFromJson(String json) {
        Object parsed = JsonParser.parse(json);
        Map<String, Object> root = object(parsed, "notification");
        if (root.containsKey("data")) {
            return gameInfo(object(root.get("data"), "notification data"));
        }
        return gameInfo(root);
    }

    private static Object decodeData(String operation, Object rawData) {
        if (rawData == null) {
            return null;
        }
        Map<String, Object> data = object(rawData, "data");
        return switch (operation) {
            case "register" -> new RegisterData(string(data, "username"));
            case "login" -> new LoginData(string(data, "accountToken"));
            case "logout" -> new LogoutData();
            case "updateCredentials" -> new UpdateCredentialsData(string(data, "newUsername"));
            case "submitProposal", "requestGameInfo" -> gameInfo(data);
            case "requestGameStats" -> gameStats(data);
            case "requestLeaderboard" -> leaderboard(data);
            case "requestPlayerStats" -> playerStats(data);
            default -> rawData;
        };
    }

    private static ApiError error(Map<String, Object> data) {
        String code = string(data, "code");
        ErrorCode errorCode = ErrorCode.valueOf(code);
        return new ApiError(errorCode, nullableString(data.get("message")));
    }

    private static GameInfoData gameInfo(Map<String, Object> data) {
        List<String> words = stringList(data.get("words"));
        List<java.util.Set<String>> correctGuesses = setList(data.get("correctGuesses"));
        List<java.util.Set<String>> wrongGuesses = setList(data.get("wrongGuesses"));
        List<List<String>> correctGroups = data.get("correctGroups") == null
                ? null : nestedStringList(data.get("correctGroups"));
        return new GameInfoData(
                longValue(data, "gameId"),
                longValue(data, "expiresAt"),
                words,
                correctGuesses,
                wrongGuesses,
                correctGroups);
    }

    private static GameStatsData gameStats(Map<String, Object> data) {
        return new GameStatsData(
                longValue(data, "gameId"),
                bool(data, "completed"),
                longValue(data, "expiresAt"),
                integer(data, "totalParticipants", 0),
                integer(data, "activePlayers", 0),
                integer(data, "completedPlayers", 0),
                integer(data, "winners", 0),
                doubleValue(data, "averageScore", 0));
    }

    private static LeaderboardData leaderboard(Map<String, Object> data) {
        List<LeaderboardEntry> topPlayers = leaderboardEntries(data.get("topPlayers"));
        LeaderboardEntry requestedPlayer = data.get("requestedPlayer") == null
                ? null : leaderboardEntry(object(data.get("requestedPlayer"), "requestedPlayer"));
        return new LeaderboardData(topPlayers, requestedPlayer, integer(data, "totalPlayers", 0));
    }

    private static PlayerStatsData playerStats(Map<String, Object> data) {
        Map<Integer, Integer> histogram = new LinkedHashMap<>();
        Map<String, Object> raw = object(data.get("mistakeHistogram"), "mistakeHistogram");
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            histogram.put(Integer.parseInt(entry.getKey()), integerValue(entry.getValue()));
        }
        return new PlayerStatsData(
                integer(data, "puzzlesCompleted", 0),
                doubleValue(data, "winRate", 0),
                doubleValue(data, "lossRate", 0),
                integer(data, "currentStreak", 0),
                integer(data, "maxStreak", 0),
                integer(data, "perfectPuzzles", 0),
                histogram);
    }

    private static List<LeaderboardEntry> leaderboardEntries(Object value) {
        List<LeaderboardEntry> result = new ArrayList<>();
        if (value == null) {
            return result;
        }
        for (Object entry : list(value, "leaderboard")) {
            result.add(leaderboardEntry(object(entry, "leaderboard entry")));
        }
        return result;
    }

    private static LeaderboardEntry leaderboardEntry(Map<String, Object> data) {
        return new LeaderboardEntry(
                string(data, "username"),
                integer(data, "score", 0),
                integer(data, "rank", 0));
    }

    private static List<java.util.Set<String>> setList(Object value) {
        List<java.util.Set<String>> result = new ArrayList<>();
        if (value == null) {
            return result;
        }
        for (Object item : list(value, "set list")) {
            result.add(new LinkedHashSet<>(stringList(item)));
        }
        return result;
    }

    private static List<List<String>> nestedStringList(Object value) {
        List<List<String>> result = new ArrayList<>();
        for (Object item : list(value, "nested string list")) {
            result.add(stringList(item));
        }
        return result;
    }

    private static List<String> stringList(Object value) {
        List<String> result = new ArrayList<>();
        for (Object item : list(value, "string list")) {
            if (!(item instanceof String string)) {
                throw new IllegalArgumentException("Expected JSON string in list but found: " + item);
            }
            result.add(string);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value, String context) {
        if (!(value instanceof Map<?, ?> raw)) {
            throw new IllegalArgumentException("Expected JSON object for " + context);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new IllegalArgumentException("Expected string JSON key in " + context);
            }
            result.put(key, entry.getValue());
        }
        return result;
    }

    private static List<?> list(Object value, String context) {
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException("Expected JSON array for " + context);
        }
        return list;
    }

    private static String string(Map<String, Object> data, String key) {
        String value = nullableString(data.get(key));
        if (value == null) {
            throw new IllegalArgumentException("Missing string field: " + key);
        }
        return value;
    }

    private static String nullableString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static boolean bool(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (!(value instanceof Boolean booleanValue)) {
            throw new IllegalArgumentException("Expected boolean field: " + key);
        }
        return booleanValue;
    }

    private static int integer(Map<String, Object> data, String key, int defaultValue) {
        Object value = data.get(key);
        return value == null ? defaultValue : integerValue(value);
    }

    private static Integer nullableInteger(Object value) {
        return value == null ? null : integerValue(value);
    }

    private static int integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalArgumentException("Expected integer but found: " + value);
    }

    private static long longValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Expected long field: " + key);
        }
        return number.longValue();
    }

    private static Long nullableLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static double doubleValue(Map<String, Object> data, String key, double defaultValue) {
        Object value = data.get(key);
        return value == null ? defaultValue : ((Number) value).doubleValue();
    }
}
