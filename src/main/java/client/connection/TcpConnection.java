package client.connection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

/**
 * Manages a blocking TCP connection to the server with support for sending and receiving
 * newline-delimited JSON messages.
 */
public final class TcpConnection {

  private static final int READ_TIMEOUT_MILLIS = 10_000;

  private SocketChannel channel;
  private BufferedReader reader;
  private BufferedWriter writer;

  /**
   * Establishes a connection to the specified host and port.
   *
   * @param host the server host name or IP address
   * @param port the server TCP port
   * @throws IOException if an I/O error occurs during connection
   */
  public void connect(String host, int port) throws IOException {
    close();

    SocketChannel newChannel = SocketChannel.open();
    newChannel.configureBlocking(true);
    try {
      newChannel.connect(new InetSocketAddress(host, port));
      newChannel.socket().setSoTimeout(READ_TIMEOUT_MILLIS);
    } catch (IOException e) {
      newChannel.close();
      throw e;
    }

    channel = newChannel;
    reader =
        new BufferedReader(
            new InputStreamReader(channel.socket().getInputStream(), StandardCharsets.UTF_8));
    writer = new BufferedWriter(Channels.newWriter(newChannel, StandardCharsets.UTF_8));
  }

  /**
   * Sends a JSON string and waits for a newline-delimited response.
   *
   * @param json the JSON request to send
   * @return the JSON response received
   * @throws IOException if an I/O error occurs or a timeout happens
   */
  public synchronized String send(String json) throws IOException {
    ensureConnected();
    writer.write(json);
    writer.newLine();
    writer.flush();
    try {
      return reader.readLine();
    } catch (SocketTimeoutException e) {
      throw new IOException("Server did not respond within " + READ_TIMEOUT_MILLIS + "ms", e);
    }
  }

  /**
   * Closes the underlying channel and releases resources.
   *
   * @throws IOException if an I/O error occurs while closing
   */
  public void close() throws IOException {
    if (channel != null) {
      try {
        channel.close();
      } finally {
        channel = null;
        reader = null;
        writer = null;
      }
    }
  }

  /**
   * Checks if the connection is open.
   *
   * @throws IOException if the channel is not open
   */
  private void ensureConnected() throws IOException {
    if (channel == null || !channel.isOpen()) {
      throw new IOException("Client is not connected to the server");
    }
  }
}
