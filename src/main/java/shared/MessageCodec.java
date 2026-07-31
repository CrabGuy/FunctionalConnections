package shared;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class MessageCodec {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MessageCodec() {}

    public static <T> String serialize(T object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Serialization error", e);
        }
    }

    public static <T> T deserialize(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Deserialization error", e);
        }
    }

    public static String serializeError(String errorMessage) {
        return serialize(new Response(false, null, errorMessage));
    }
}