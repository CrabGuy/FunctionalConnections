package client.json;

import com.google.gson.Gson;
import shared.dto.*;
import java.lang.reflect.Type;
import java.util.*;

public final class ProtocolCodec {
    private static final Gson gson = new Gson();
    private static final Type MAP_TYPE = new com.google.gson.reflect.TypeToken<Map<String, Object>>() {}.getType();

    private ProtocolCodec() {}

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

    public static ApiRequest requestFromJson(String json) {
        Map<String, Object> object = gson.fromJson(json, MAP_TYPE);
        String operation = (String) object.get("operation");
        return switch (operation) {
            case "register" -> new RegisterRequest((String) object.get("username"), (String) object.get("psw"));
            case "updateCredentials" -> new UpdateCredentialsRequest(
                    (String) object.get("oldUsername"),
                    (String) object.get("newUsername"),
                    (String) object.get("oldPsw"),
                    (String) object.get("newPsw")
            );
            case "login" -> new LoginRequest(
                    (String) object.get("username"),
                    (String) object.get("psw"),
                    ((Number) object.getOrDefault("udpPort", 0)).intValue()
            );
            case "logout" -> new LogoutRequest((String) object.get("accountToken"));
            case "submitProposal" -> new SubmitProposalRequest(
                    (String) object.get("accountToken"),
                    stringList(object.get("words"))
            );
            case "requestGameInfo" -> new RequestGameInfoRequest(
                    (String) object.get("accountToken"),
                    nullableLong(object.get("gameId"))
            );
            case "requestGameStats" -> new RequestGameStatsRequest(
                    (String) object.get("accountToken"),
                    nullableLong(object.get("gameId"))
            );
            case "requestLeaderboard" -> new RequestLeaderboardRequest(
                    (String) object.get("accountToken"),
                    nullableString(object.get("playerName")),
                    nullableInteger(object.get("topPlayers"))
            );
            case "requestPlayerStats" -> new RequestPlayerStatsRequest((String) object.get("accountToken"));
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        };
    }

    public static ApiResponse<?> responseFromJson(String json, String expectedOperation) {
        Map<String, Object> object = gson.fromJson(json, MAP_TYPE);
        boolean success = (boolean) object.get("success");
        ApiError error = object.get("error") == null ? null : error((Map<String, Object>) object.get("error"));
        Object rawData = object.get("data");
        Object data = success ? decodeData(expectedOperation, rawData) : rawData;
        return new ApiResponse<>(success, error, data);
    }

    public static GameInfoData gameInfoFromJson(String json) {
        Map<String, Object> root = gson.fromJson(json, MAP_TYPE);
        if (root.containsKey("data")) {
            return gameInfo((Map<String, Object>) root.get("data"));
        }
        return gameInfo(root);
    }

    private static Object decodeData(String operation, Object rawData) {
        if (rawData == null) return null;
        Map<String, Object> data = (Map<String, Object>) rawData;
        return switch (operation) {
            case "register" -> new RegisterData((String) data.get("username"));
            case "login" -> new LoginData((String) data.get("accountToken"));
            case "logout" -> new LogoutData();
            case "updateCredentials" -> new UpdateCredentialsData((String) data.get("newUsername"));
            case "submitProposal", "requestGameInfo" -> gameInfo(data);
            case "requestGameStats" -> gameStats(data);
            case "requestLeaderboard" -> leaderboard(data);
            case "requestPlayerStats" -> playerStats(data);
            default -> rawData;
        };
    }

    private static ApiError error(Map<String, Object> data) {
        String code = (String) data.get("code");
        ErrorCode errorCode = ErrorCode.valueOf(code);
        return new ApiError(errorCode, (String) data.get("message"));
    }

    private static GameInfoData gameInfo(Map<String, Object> data) {
        List<String> words = stringList(data.get("words"));
        List<Set<String>> correctGuesses = setList(data.get("correctGuesses"));
        List<Set<String>> wrongGuesses = setList(data.get("wrongGuesses"));
        List<List<String>> correctGroups = data.get("correctGroups") == null
                ? null : nestedStringList(data.get("correctGroups"));
        return new GameInfoData(
                longValue(data, "gameId"),
                longValue(data, "expiresAt"),
                words,
                correctGuesses,
                wrongGuesses,
                correctGroups
        );
    }

    private static GameStatsData gameStats(Map<String, Object> data) {
        return new GameStatsData(
                longValue(data, "gameId"),
                (boolean) data.get("completed"),
                longValue(data, "expiresAt"),
                integerValue(data.getOrDefault("totalParticipants", 0)),
                integerValue(data.getOrDefault("activePlayers", 0)),
                integerValue(data.getOrDefault("completedPlayers", 0)),
                integerValue(data.getOrDefault("winners", 0)),
                doubleValue(data.getOrDefault("averageScore", 0.0))
        );
    }

    private static LeaderboardData leaderboard(Map<String, Object> data) {
        List<LeaderboardEntry> topPlayers = leaderboardEntries(data.get("topPlayers"));
        LeaderboardEntry requestedPlayer = data.get("requestedPlayer") == null
                ? null : leaderboardEntry((Map<String, Object>) data.get("requestedPlayer"));
        return new LeaderboardData(topPlayers, requestedPlayer, integerValue(data.getOrDefault("totalPlayers", 0)));
    }

    private static PlayerStatsData playerStats(Map<String, Object> data) {
        Map<Integer, Integer> histogram = new LinkedHashMap<>();
        Map<String, Object> raw = (Map<String, Object>) data.get("mistakeHistogram");
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            histogram.put(Integer.parseInt(entry.getKey()), integerValue(entry.getValue()));
        }
        return new PlayerStatsData(
                integerValue(data.getOrDefault("puzzlesCompleted", 0)),
                doubleValue(data.getOrDefault("winRate", 0.0)),
                doubleValue(data.getOrDefault("lossRate", 0.0)),
                integerValue(data.getOrDefault("currentStreak", 0)),
                integerValue(data.getOrDefault("maxStreak", 0)),
                integerValue(data.getOrDefault("perfectPuzzles", 0)),
                histogram
        );
    }

    private static List<LeaderboardEntry> leaderboardEntries(Object value) {
        List<LeaderboardEntry> result = new ArrayList<>();
        if (value == null) return result;
        for (Object entry : (List<?>) value) {
            result.add(leaderboardEntry((Map<String, Object>) entry));
        }
        return result;
    }

    private static LeaderboardEntry leaderboardEntry(Map<String, Object> data) {
        return new LeaderboardEntry(
                (String) data.get("username"),
                integerValue(data.getOrDefault("score", 0)),
                integerValue(data.getOrDefault("rank", 0))
        );
    }

    private static List<Set<String>> setList(Object value) {
        List<Set<String>> result = new ArrayList<>();
        if (value == null) return result;
        for (Object item : (List<?>) value) {
            result.add(new LinkedHashSet<>(stringList(item)));
        }
        return result;
    }

    private static List<List<String>> nestedStringList(Object value) {
        List<List<String>> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            result.add(stringList(item));
        }
        return result;
    }

    private static List<String> stringList(Object value) {
        List<String> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            result.add((String) item);
        }
        return result;
    }

    private static String nullableString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Integer nullableInteger(Object value) {
        return value == null ? null : integerValue(value);
    }

    private static Long nullableLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static int integerValue(Object value) {
        return ((Number) value).intValue();
    }

    private static long longValue(Map<String, Object> data, String key) {
        return ((Number) data.get(key)).longValue();
    }

    private static double doubleValue(Object value) {
        return ((Number) value).doubleValue();
    }
}