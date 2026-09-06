package server.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import shared.dto.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;

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
            writer.close();
        } finally {
            try {
                clientChannel.close();
            } catch (IOException ignored) {}
        }
    }

    private ApiRequest parseRequest(String json) {
        JsonObject obj = gson.fromJson(json, JsonObject.class);
        String operation = obj.get("operation").getAsString();
        return switch (operation) {
            case "register" -> gson.fromJson(json, RegisterRequest.class);
            case "updateCredentials" -> gson.fromJson(json, UpdateCredentialsRequest.class);
            case "login" -> gson.fromJson(json, LoginRequest.class);
            case "logout" -> gson.fromJson(json, LogoutRequest.class);
            case "submitProposal" -> gson.fromJson(json, SubmitProposalRequest.class);
            case "requestGameInfo" -> gson.fromJson(json, RequestGameInfoRequest.class);
            case "requestGameStats" -> gson.fromJson(json, RequestGameStatsRequest.class);
            case "requestLeaderboard" -> gson.fromJson(json, RequestLeaderboardRequest.class);
            case "requestPlayerStats" -> gson.fromJson(json, RequestPlayerStatsRequest.class);
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        };
    }
}
