package server.dto;

import java.util.List;

/**
 * Represents a group of four words with a common theme.
 *
 * @param theme the theme name
 * @param words the list of words (typically four)
 */
public record WordGroup(String theme, List<String> words) {

  public WordGroup {
    words = List.copyOf(words);
  }
}
