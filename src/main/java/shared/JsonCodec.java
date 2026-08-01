package shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JavaType;

public final class JsonCodec {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonCodec() {}

    public static <T> String serialize(T object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Serialization error", e);
        }
    }

    public static <T> T deserialize(String json, Class<T> clazz) {
        try {
            var type = MAPPER.getTypeFactory().constructType(clazz);
            @SuppressWarnings("unchecked")
            T result = (T) MAPPER.readValue(json, type);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Deserialization error", e);
        }
    }

    public static <T> T deserialize(String json, JavaType type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException("Deserialization error", e);
        }
    }

    public static String serializeError(String errorMessage) {
        return serialize(new Response(false, null, errorMessage));
    }
}