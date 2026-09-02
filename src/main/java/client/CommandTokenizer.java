package client;

import java.util.ArrayList;
import java.util.List;

/** Splits a CLI line into whitespace-separated arguments with basic quote support. */
public final class CommandTokenizer {
    private CommandTokenizer() {
    }

    public static List<String> tokenize(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        boolean quoted = false;
        char quote = 0;
        boolean escaping = false;

        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (escaping) {
                token.append(current);
                escaping = false;
                continue;
            }
            if (current == '\\' && quoted) {
                escaping = true;
                continue;
            }
            if (quoted) {
                if (current == quote) {
                    quoted = false;
                } else {
                    token.append(current);
                }
                continue;
            }
            if (current == '"' || current == '\'') {
                quoted = true;
                quote = current;
            } else if (Character.isWhitespace(current)) {
                if (!token.isEmpty()) {
                    tokens.add(token.toString());
                    token.setLength(0);
                }
            } else {
                token.append(current);
            }
        }

        if (escaping || quoted) {
            throw new IllegalArgumentException("Unterminated quote or escape sequence");
        }
        if (!token.isEmpty()) {
            tokens.add(token.toString());
        }
        return List.copyOf(tokens);
    }
}
