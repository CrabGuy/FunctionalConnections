package server.storage;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import shared.JsonCodec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class MapStorage {
    private MapStorage() {}

    public static <K, V> void save(Path path, Map<K, V> snapshot) {
        try {
            Map<K, V> snapshotCopy = Map.copyOf(snapshot);
            Files.createDirectories(path.getParent());
            Path tempFile = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
            Files.writeString(tempFile, JsonCodec.serialize(snapshotCopy));
            Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write JSON to file: " + path, e);
        }
    }

    public static <T> void save(Path path, T object) {
        try {
            Files.createDirectories(path.getParent());
            Path tempFile = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
            Files.writeString(tempFile, JsonCodec.serialize(object));
            Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write JSON to file: " + path, e);
        }
    }

    public static <K, V> Map<K, V> load(Path path, JavaType type) {
        if (!Files.exists(path)) return new HashMap<>();
        try {
            String content = Files.readString(path);
            if (content.isBlank()) return new HashMap<>();
            return JsonCodec.deserialize(content, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON from file: " + path, e);
        } catch (RuntimeException e) {
            quarantineCorruptFile(path, e);
            return new HashMap<>();
        }
    }

    public static <K, V> Map<K, V> load(Path path, Class<K> keyClass, Class<V> valueClass) {
        JavaType type = TypeFactory.defaultInstance().constructMapType(Map.class, keyClass, valueClass);
        return load(path, type);
    }

    public static <T> T load(Path path, Class<T> clazz) {
        if (!Files.exists(path)) return null;
        try {
            String content = Files.readString(path);
            if (content.isBlank()) return null;
            return JsonCodec.deserialize(content, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON from file: " + path, e);
        } catch (RuntimeException e) {
            quarantineCorruptFile(path, e);
            return null;
        }
    }

    private static void quarantineCorruptFile(Path path, RuntimeException cause) {
        try {
            Path backup = path.resolveSibling(path.getFileName() + ".corrupt-" + Instant.now().toEpochMilli());
            Files.move(path, backup, StandardCopyOption.REPLACE_EXISTING);
            System.err.println("Corrupt storage file quarantined at " + backup + ", starting fresh for " + path + ": " + cause.getMessage());
        } catch (IOException quarantineFailure) {
            System.err.println("Could not quarantine corrupt storage file " + path + ": " + quarantineFailure.getMessage());
        }
    }
}