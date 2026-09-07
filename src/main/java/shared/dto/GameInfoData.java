package shared.dto;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public record GameInfoData(
    long gameId,
    long expiresAt,
    List<String> words,
    List<Set<String>> correctGuesses,
    List<Set<String>> wrongGuesses,
    List<List<String>> correctGroups) {
  public GameInfoData {
    words = List.copyOf(words);
    correctGuesses = correctGuesses.stream().map(Set::copyOf).toList();
    wrongGuesses = wrongGuesses.stream().map(Set::copyOf).toList();
    if (correctGroups != null) {
      correctGroups = correctGroups.stream().map(List::copyOf).toList();
    }
  }

  public Optional<List<List<String>>> correctGroupsOptional() {
    return Optional.ofNullable(correctGroups);
  }
}
