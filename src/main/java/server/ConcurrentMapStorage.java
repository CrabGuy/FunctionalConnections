package server;

import shared.JsonCodec;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;
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
        if (!Files.exists(path)) return new ConcurrentHashMap<>();
        JavaType mapType = TypeFactory.defaultInstance()
            .constructMapType(ConcurrentHashMap.class, keyClass, valueClass);
        try {
            return JsonCodec.deserialize(Files.readString(path), mapType);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON from file: " + path, e);
        }
    }
}