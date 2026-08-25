package server;
import shared.JsonCodec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
public final class ConcurrentMapStorage {
    private ConcurrentMapStorage() {}
    public static <K, V> void save(Path path, ConcurrentHashMap<K, V> map) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, JsonCodec.serialize(map));
        } catch (Exception e) {
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
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read JSON from file: " + path, e);
        }
    }
}