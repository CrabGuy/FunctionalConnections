package server.dto;

import java.util.List;

/**
 * Represents a thematic group of four words that belong together.
 * Immutable.
 *
 * @param theme the theme of the group (e.g., "Colors").
 * @param words the four words in this group.
 */
public record WordGroup(String theme, List<String> words) {
    public WordGroup {
        words = List.copyOf(words);
    }
}
