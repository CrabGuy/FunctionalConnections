package shared;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonCodec {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.findAndRegisterModules();
        MAPPER.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        MAPPER.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        MAPPER.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);
        MAPPER.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE);
        MAPPER.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY);
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private JsonCodec() {}

    public static <T> String serialize(T object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Serialization error: " + e.getMessage(), e);
        }
    }

    public static <T> T deserialize(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Deserialization error: " + e.getMessage(), e);
        }
    }

    public static <T> T deserialize(String json, JavaType type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException("Deserialization error: " + e.getMessage(), e);
        }
    }

    public static <T> Response<T> deserializeResponse(String json, Class<T> resultClass) {
        try {
            if (resultClass == Void.class || resultClass == null) {
                JavaType type = MAPPER.getTypeFactory().constructParametricType(Response.class, Object.class);
                return MAPPER.readValue(json, type);
            }
            JavaType type = MAPPER.getTypeFactory().constructParametricType(Response.class, resultClass);
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException("Response deserialization error: " + e.getMessage(), e);
        }
    }

    public static String serializeError(String errorMessage) {
        return serialize(Response.error(errorMessage));
    }
}