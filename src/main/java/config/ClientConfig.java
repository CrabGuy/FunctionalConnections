package config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record ClientConfig(
        String serverHost,
        int serverPort,
        int udpPort
) {
    public static ClientConfig load() throws IOException {
        Path propsPath = Path.of("client.properties");
        if (!Files.exists(propsPath)) {
            throw new IOException("Missing client.properties file in working directory.");
        }
        Properties props = new Properties();
        try (var reader = Files.newBufferedReader(propsPath)) {
            props.load(reader);
        }

        String serverHost = props.getProperty("client.server.host", "localhost");
        int serverPort = getInt(props, "client.server.port", 8080);
        int udpPort = getInt(props, "client.udp.port", 9876);

        return new ClientConfig(serverHost, serverPort, udpPort);
    }

    private static int getInt(Properties props, String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer for key " + key + ": " + value);
        }
    }
}