package config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

public record ServerConfig(
        int tcpPort,
        int udpPort,
        Duration gameDuration,
        int maxMistakes,
        int saveIntervalSeconds,
        Path storageDir,
        String puzzleFilePath,
        int maxInMemoryGames
) {
    public static ServerConfig load() throws IOException {
        Path propsPath = Path.of("server.properties");
        if (!Files.exists(propsPath)) {
            throw new IOException("Missing server.properties file in working directory.");
        }
        Properties props = new Properties();
        try (var reader = Files.newBufferedReader(propsPath)) {
            props.load(reader);
        }

        int tcpPort = getInt(props, "server.tcp.port", 8080);
        int udpPort = getInt(props, "server.udp.port", 9876);
        long gameDurationSeconds = getLong(props, "server.game.duration.seconds", 600);
        int maxMistakes = getInt(props, "server.max.mistakes", 4);
        int saveIntervalSeconds = getInt(props, "server.save.interval.seconds", 30);
        Path storageDir = Path.of(getString(props, "server.storage.dir", "storage"));
        String puzzleFilePath = getString(props, "server.puzzle.file", "Connections_Data.json");
        int maxInMemoryGames = getInt(props, "server.max.in.memory.games", 10);

        return new ServerConfig(
                tcpPort,
                udpPort,
                Duration.ofSeconds(gameDurationSeconds),
                maxMistakes,
                saveIntervalSeconds,
                storageDir,
                puzzleFilePath,
                maxInMemoryGames
        );
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

    private static long getLong(Properties props, String key, long defaultValue) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid long for key " + key + ": " + value);
        }
    }

    private static String getString(Properties props, String key, String defaultValue) {
        String value = props.getProperty(key);
        return (value == null || value.isBlank()) ? defaultValue : value.trim();
    }
}