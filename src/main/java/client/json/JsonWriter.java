package client.json;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/** Dependency-free JSON writer for simple protocol values. */
public final class JsonWriter {
    private JsonWriter() {
    }

    public static String write(Object value) {
        StringBuilder output = new StringBuilder();
        writeValue(value, output);
        return output.toString();
    }

    private static void writeValue(Object value, StringBuilder output) {
        if (value == null) {
            output.append("null");
        } else if (value instanceof String string) {
            writeString(string, output);
        } else if (value instanceof Number || value instanceof Boolean) {
            output.append(value);
        } else if (value instanceof Map<?, ?> map) {
            writeMap(map, output);
        } else if (value instanceof Collection<?> collection) {
            writeCollection(collection.iterator(), output);
        } else if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            output.append('[');
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    output.append(',');
                }
                writeValue(Array.get(value, i), output);
            }
            output.append(']');
        } else {
            throw new IllegalArgumentException("Unsupported JSON value type: " + value.getClass());
        }
    }

    private static void writeMap(Map<?, ?> map, StringBuilder output) {
        output.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new IllegalArgumentException("JSON object keys must be strings");
            }
            if (!first) {
                output.append(',');
            }
            first = false;
            writeString(key, output);
            output.append(':');
            writeValue(entry.getValue(), output);
        }
        output.append('}');
    }

    private static void writeCollection(Iterator<?> values, StringBuilder output) {
        output.append('[');
        boolean first = true;
        while (values.hasNext()) {
            if (!first) {
                output.append(',');
            }
            first = false;
            writeValue(values.next(), output);
        }
        output.append(']');
    }

    private static void writeString(String value, StringBuilder output) {
        output.append('"');
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"' -> output.append("\\\"");
                case '\\' -> output.append("\\\\");
                case '\b' -> output.append("\\b");
                case '\f' -> output.append("\\f");
                case '\n' -> output.append("\\n");
                case '\r' -> output.append("\\r");
                case '\t' -> output.append("\\t");
                default -> {
                    if (character < 0x20) {
                        output.append(String.format("\\u%04x", (int) character));
                    } else {
                        output.append(character);
                    }
                }
            }
        }
        output.append('"');
    }
}
