package server;
import shared.JsonCodec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
public final class ConcurrentMapStorage {
    private ConcurrentMapStorage() {}
    public static <K, V> void save(Path path, ConcurrentHashMap<K, V> map) {
        try {
            Files.createDirectories(path.getParent());
            Path tempFile = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
            Files.writeString(tempFile, JsonCodec.serialize(map));
            Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write JSON to file: " + path, e);
        }
    }
    public static <K, V> ConcurrentHashMap<K, V> load(Path path, Class<K> keyClass, Class<V> valueClass) {
        if (!Files.exists(path)) {
            return new ConcurrentHashMap<>();
        }
        try {
            String content = Files.readString(path);
            if (content.isBlank()) {
                return new ConcurrentHashMap<>();
            }
            var mapType = com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance()
                    .constructMapType(ConcurrentHashMap.class, keyClass, valueClass);
            return JsonCodec.deserialize(content, mapType);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON from file: " + path, e);
        } catch (RuntimeException e) {
            quarantineCorruptFile(path, e);
            return new ConcurrentHashMap<>();
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