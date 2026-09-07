package shared.dto;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Contains all information about a game for a specific player, including word list, guesses, and
 * optionally the correct groups.
 *
 * @param gameId the game ID
 * @param expiresAt the expiration time in milliseconds
 * @param words the list of all words in the game (shuffled)
 * @param correctGuesses list of correct guess sets
 * @param wrongGuesses list of wrong guess sets
 * @param correctGroups list of correct groups (only when game ended or won/lost)
 */
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

  /**
   * Returns the correct groups as an Optional.
   *
   * @return an Optional containing the correct groups, or empty if not available
   */
  public Optional<List<List<String>>> correctGroupsOptional() {
    return Optional.ofNullable(correctGroups);
  }
}
