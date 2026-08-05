package server;

import shared.JsonCodec;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class ConcurrentMapStorage {

    private ConcurrentMapStorage() {}

    public static <K, V> void save(Path path, ConcurrentHashMap<K, V> map) {
        Function<ConcurrentHashMap<K, V>, String> serialize = JsonCodec::serialize;
        
        try {
            Files.writeString(path, serialize.apply(map));
        } catch (Exception e) {
            throw new RuntimeException("Failed to write JSON to file: " + path, e);
        }
    }

    public static <K, V> ConcurrentHashMap<K, V> load(Path path, Class<K> keyClass, Class<V> valueClass) {
        JavaType mapType = TypeFactory.defaultInstance()
                .constructMapType(ConcurrentHashMap.class, keyClass, valueClass);

        Function<Path, String> readPath = p -> {
            try {
                return Files.readString(p);
            } catch (Exception e) {
                throw new RuntimeException("Failed to read JSON from file: " + p, e);
            }
        };

        return Files.exists(path)
                ? readPath.andThen((String json) -> JsonCodec.<ConcurrentHashMap<K, V>>deserialize(json, mapType)).apply(path)
                : new ConcurrentHashMap<>();
    }
}