package server.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import shared.dto.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public final class ConnectionHandlerImpl implements ConnectionHandler {
    private final Gson gson;
    private final RequestDispatcher dispatcher;
    private SocketChannel clientChannel;

    public ConnectionHandlerImpl(RequestDispatcher dispatcher, Gson gson) {
        this.dispatcher = dispatcher;
        this.gson = gson;
    }

    @Override
    public void bind(SocketChannel clientChannel) {
        this.clientChannel = clientChannel;
    }

    @Override
    public void run() {
        if (clientChannel == null) {
            throw new IllegalStateException("bind() must be called before run()");
        }
        try (BufferedReader reader = new BufferedReader(Channels.newReader(clientChannel, "UTF-8"));
            PrintWriter writer = new PrintWriter(Channels.newWriter(clientChannel, "UTF-8"), true)) {
            String line;
            while ((line = reader.readLine()) != null) {
                ApiRequest request = parseRequest(line);
                ApiResponse<?> response = dispatcher.dispatch(request,
                        (InetSocketAddress) clientChannel.getRemoteAddress());
                writer.println(gson.toJson(response));
            }
        } catch (Exception e) {
            ApiError error = new ApiError(ErrorCode.INTERNAL_ERROR, "Invalid request format");
            ApiResponse<?> response = new ApiResponse<>(false, error, null);
            PrintWriter writer = new PrintWriter(Channels.newWriter(clientChannel, "UTF-8"), true);
            writer.println(gson.toJson(response));
        } finally {
            try {
                clientChannel.close();
            } catch (IOException ignored) {}
        }
    }
    /**
     * Parses a JSON string into the correct ApiRequest subtype based on the "operation" field.
     */
    private ApiRequest parseRequest(String json) {
        JsonObject obj = gson.fromJson(json, JsonObject.class);
        String operation = obj.get("operation").getAsString();
        return switch (operation) {
            case "register" -> new RegisterRequest(
                    obj.get("username").getAsString(),
                    obj.get("psw").getAsString()
            );
            case "updateCredentials" -> new UpdateCredentialsRequest(
                    obj.get("oldUsername").getAsString(),
                    obj.get("newUsername").getAsString(),
                    obj.get("oldPsw").getAsString(),
                    obj.get("newPsw").getAsString()
            );
            case "login" -> new LoginRequest(
                    obj.get("username").getAsString(),
                    obj.get("psw").getAsString(),
                    obj.has("udpPort") ? obj.get("udpPort").getAsInt() : 0
            );
            case "logout" -> new LogoutRequest(obj.get("accountToken").getAsString());
            case "submitProposal" -> new SubmitProposalRequest(
                    obj.get("accountToken").getAsString(),
                    obj.has("gameId") ? obj.get("gameId").getAsLong() : null,
                    stringList(obj.get("words"))
            );
            case "requestGameInfo" -> new RequestGameInfoRequest(
                    obj.get("accountToken").getAsString(),
                    obj.has("gameId") ? obj.get("gameId").getAsLong() : null
            );
            case "requestGameStats" -> new RequestGameStatsRequest(
                    obj.get("accountToken").getAsString(),
                    obj.has("gameId") ? obj.get("gameId").getAsLong() : null
            );
            case "requestLeaderboard" -> new RequestLeaderboardRequest(
                    obj.get("accountToken").getAsString(),
                    obj.has("playerName") ? obj.get("playerName").getAsString() : null,
                    obj.has("topPlayers") ? obj.get("topPlayers").getAsInt() : null
            );
            case "requestPlayerStats" -> new RequestPlayerStatsRequest(obj.get("accountToken").getAsString());
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        };
    }

    private List<String> stringList(JsonElement element) {
        List<String> result = new ArrayList<>();
        if (element != null && element.isJsonArray()) {
            for (JsonElement e : element.getAsJsonArray()) {
                result.add(e.getAsString());
            }
        }
        return result;
    }
}