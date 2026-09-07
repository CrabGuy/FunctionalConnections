package server.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import shared.dto.ApiError;
import shared.dto.ApiRequest;
import shared.dto.ApiResponse;
import shared.dto.ErrorCode;
import shared.dto.LoginRequest;
import shared.dto.LogoutRequest;
import shared.dto.RegisterRequest;
import shared.dto.RequestGameInfoRequest;
import shared.dto.RequestGameStatsRequest;
import shared.dto.RequestLeaderboardRequest;
import shared.dto.RequestPlayerStatsRequest;
import shared.dto.SubmitProposalRequest;
import shared.dto.UpdateCredentialsRequest;

public class ClientConnection {
  private final SocketChannel channel;
  private final RequestDispatcher dispatcher;
  private final Gson gson;
  private final Selector selector;
  private final ByteBuffer readBuffer = ByteBuffer.allocate(8192);
  private final Queue<ByteBuffer> writeQueue = new ArrayDeque<>();
  private boolean writePending = false;

  public ClientConnection(
      SocketChannel channel, RequestDispatcher dispatcher, Gson gson, Selector selector) {
    this.channel = channel;
    this.dispatcher = dispatcher;
    this.gson = gson;
    this.selector = selector;
  }

  public void handleRead(ExecutorService workerPool) {
    try {
      int bytesRead = channel.read(readBuffer);
      if (bytesRead == -1) {
        close();
        return;
      }
      readBuffer.flip();
      while (true) {
        int newlinePos = findNewline(readBuffer);
        if (newlinePos == -1) break;
        byte[] requestBytes = new byte[newlinePos - readBuffer.position()];
        readBuffer.get(requestBytes);
        readBuffer.get();
        String requestJson = new String(requestBytes, StandardCharsets.UTF_8);
        workerPool.submit(() -> processRequest(requestJson));
      }
      readBuffer.compact();
    } catch (IOException e) {
      close();
    }
  }

  public void handleWrite() {
    try {
      while (!writeQueue.isEmpty()) {
        ByteBuffer buf = writeQueue.peek();
        channel.write(buf);
        if (buf.hasRemaining()) {
          return;
        }
        writeQueue.poll();
      }
      writePending = false;
      SelectionKey key = channel.keyFor(selector);
      if (key != null) {
        key.interestOps(SelectionKey.OP_READ);
      }
    } catch (IOException e) {
      close();
    }
  }

  private void processRequest(String requestJson) {
    try {
      ApiRequest request = parseRequest(requestJson);
      InetSocketAddress remoteAddress = (InetSocketAddress) channel.getRemoteAddress();
      ApiResponse<?> response = dispatcher.dispatch(request, remoteAddress);
      String responseJson = gson.toJson(response);
      byte[] responseBytes = responseJson.getBytes(StandardCharsets.UTF_8);
      ByteBuffer outBuffer = ByteBuffer.allocate(responseBytes.length + 1);
      outBuffer.put(responseBytes);
      outBuffer.put((byte) '\n');
      outBuffer.flip();
      enqueueWrite(outBuffer);
    } catch (Exception e) {
      ApiError error = new ApiError(ErrorCode.INTERNAL_ERROR, "Invalid request format");
      ApiResponse<?> errorResponse = new ApiResponse<>(false, error, null);
      String errorJson = gson.toJson(errorResponse);
      ByteBuffer outBuffer = ByteBuffer.wrap((errorJson + "\n").getBytes(StandardCharsets.UTF_8));
      enqueueWrite(outBuffer);
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

  private synchronized void enqueueWrite(ByteBuffer buffer) {
    writeQueue.add(buffer);
    if (!writePending) {
      writePending = true;
      SelectionKey key = channel.keyFor(selector);
      if (key != null) {
        key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        selector.wakeup();
      }
    }
  }

  public void close() {
    try {
      channel.close();
    } catch (IOException ignored) {
    }
  }

  private int findNewline(ByteBuffer buffer) {
    int pos = buffer.position();
    while (pos < buffer.limit()) {
      if (buffer.get(pos) == '\n') {
        return pos;
      }
      pos++;
    }
    return -1;
  }
}
