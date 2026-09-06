package client.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ClientConfigLoader {
  private ClientConfigLoader() {}

  public static ClientConfig load(Path path) throws IOException {
    Properties properties = new Properties();
    try (InputStream input = Files.newInputStream(path)) {
      properties.load(input);
    }
    String serverAddress = required(properties, "serverAddress");
    int tcpPort = positivePort(properties, "tcpPort");
    int udpPort = positivePort(properties, "udpPort");
    return new ClientConfig(serverAddress, tcpPort, udpPort);
  }

  private static String required(Properties properties, String key) throws IOException {
    String value = properties.getProperty(key);
    if (value == null || value.isBlank()) {
      throw new IOException("Missing client configuration property: " + key);
    }
    return value.trim();
  }

  private static int positivePort(Properties properties, String key) throws IOException {
    int port = positivePortOrZero(properties, key);
    if (port == 0) {
      throw new IOException("Port must be greater than zero: " + key);
    }
    return port;
  }

  private static int positivePortOrZero(Properties properties, String key) throws IOException {
    String raw = required(properties, key);
    final int port;
    try {
      port = Integer.parseInt(raw);
    } catch (NumberFormatException exception) {
      throw new IOException("Invalid integer for " + key + ": " + raw, exception);
    }
    if (port < 0 || port > 65_535) {
      throw new IOException("Port out of range for " + key + ": " + port);
    }
    return port;
  }
}
