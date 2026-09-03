package server.network;

import com.google.gson.*;
import shared.dto.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public final class ApiRequestDeserializer implements JsonDeserializer<ApiRequest> {
    @Override
    public ApiRequest deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        String operation = obj.get("operation").getAsString();

        return switch (operation) {
            case "register" -> new RegisterRequest(
                    obj.get("username").getAsString(),
                    obj.get("password").getAsString()
            );
            case "login" -> new LoginRequest(
                    obj.get("username").getAsString(),
                    obj.get("password").getAsString(),
                    obj.get("udpPort").getAsInt()
            );
            case "logout" -> new LogoutRequest(
                    obj.get("accountToken").getAsString()
            );
            case "updateCredentials" -> new UpdateCredentialsRequest(
                    obj.get("oldUsername").getAsString(),
                    obj.get("newUsername").getAsString(),
                    obj.get("oldPassword").getAsString(),
                    obj.get("newPassword").getAsString()
            );
            case "submitProposal" -> {
                List<String> words = new ArrayList<>();
                for (JsonElement e : obj.getAsJsonArray("words")) {
                    words.add(e.getAsString());
                }
                Long gameId = obj.has("gameId") && !obj.get("gameId").isJsonNull()
                        ? obj.get("gameId").getAsLong()
                        : null;
                yield new SubmitProposalRequest(
                        obj.get("accountToken").getAsString(),
                        gameId,
                        words
                );
            }
            case "requestGameInfo" -> new RequestGameInfoRequest(
                    obj.get("accountToken").getAsString(),
                    obj.has("gameId") && !obj.get("gameId").isJsonNull()
                            ? obj.get("gameId").getAsLong()
                            : null
            );
            case "requestGameStats" -> new RequestGameStatsRequest(
                    obj.get("accountToken").getAsString(),
                    obj.has("gameId") && !obj.get("gameId").isJsonNull()
                            ? obj.get("gameId").getAsLong()
                            : null
            );
            case "requestLeaderboard" -> new RequestLeaderboardRequest(
                    obj.get("accountToken").getAsString(),
                    obj.has("playerName") && !obj.get("playerName").isJsonNull()
                            ? obj.get("playerName").getAsString()
                            : null,
                    obj.has("topPlayers") && !obj.get("topPlayers").isJsonNull()
                            ? obj.get("topPlayers").getAsInt()
                            : null
            );
            case "requestPlayerStats" -> new RequestPlayerStatsRequest(
                    obj.get("accountToken").getAsString()
            );
            default -> throw new JsonParseException("Unknown operation: " + operation);
        };
    }
}