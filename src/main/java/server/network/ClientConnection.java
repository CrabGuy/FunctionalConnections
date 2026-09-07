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
import shared.dto.*;

/**
 * Handles a single client TCP connection, parsing requests, dispatching them, and managing
 * asynchronous writes via a selector.
 */
public class ClientConnection {

  private final SocketChannel channel;
  private final RequestDispatcher dispatcher;
  private final Gson gson;
  private final Selector selector;
  private final ByteBuffer readBuffer = ByteBuffer.allocate(8192);
  private final Queue<ByteBuffer> writeQueue = new ArrayDeque<>();
  private boolean writePending = false;

  /**
   * Constructs a new client connection.
   *
   * @param channel the socket channel
   * @param dispatcher the request dispatcher
   * @param gson the Gson instance for JSON
   * @param selector the selector for non-blocking I/O
   */
  public ClientConnection(
      SocketChannel channel, RequestDispatcher dispatcher, Gson gson, Selector selector) {
    this.channel = channel;
    this.dispatcher = dispatcher;
    this.gson = gson;
    this.selector = selector;
  }

  /**
   * Handles a read event: reads data from the channel, extracts newline-delimited JSON requests,
   * and submits them for processing.
   *
   * @param workerPool the executor service for processing requests
   */
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
        readBuffer.get(); // consume newline
        String requestJson = new String(requestBytes, StandardCharsets.UTF_8);
        workerPool.submit(() -> processRequest(requestJson));
      }
      readBuffer.compact();
    } catch (IOException e) {
      close();
    }
  }

  /** Handles a write event: writes queued byte buffers to the channel. */
  public synchronized void handleWrite() {
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

  /**
   * Processes a single request JSON string: parses, dispatches, and enqueues the response.
   *
   * @param requestJson the JSON request
   */
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

  /**
   * Parses a JSON request into a typed {@link ApiRequest} based on the "operation" field.
   *
   * @param json the JSON string
   * @return the parsed request
   * @throws IllegalArgumentException if the operation is unknown
   */
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

  /**
   * Enqueues a byte buffer for writing and updates the selector interest ops.
   *
   * @param buffer the buffer to enqueue
   */
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

  /** Closes the socket channel. */
  public void close() {
    try {
      channel.close();
    } catch (IOException ignored) {
    }
  }

  /**
   * Finds the position of the next newline byte in the buffer.
   *
   * @param buffer the byte buffer
   * @return the index of the newline, or -1 if not found
   */
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
