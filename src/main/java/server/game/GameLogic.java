package server.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import server.dto.GameWordGroups;
import server.dto.WordGroup;

/** Utility methods for game logic such as checking correct groups and shuffling words. */
public final class GameLogic {

  private GameLogic() {
    // Prevent instantiation
  }

  /**
   * Converts the groups of a {@link GameWordGroups} into a list of sets of words.
   *
   * @param game the game word groups
   * @return a list of sets, each representing a correct group
   */
  public static List<Set<String>> correctGroupsAsSets(GameWordGroups game) {
    return game.groups().stream().map(group -> Set.copyOf(group.words())).toList();
  }

  /**
   * Converts a list of {@link WordGroup} into a list of sets of words.
   *
   * @param groups the list of word groups
   * @return a list of sets, each representing a correct group
   */
  public static List<Set<String>> correctGroupsAsSets(List<WordGroup> groups) {
    return groups.stream().map(group -> Set.copyOf(group.words())).toList();
  }

  /**
   * Checks if a proposed set of words is a correct group.
   *
   * @param proposedWords the proposed words
   * @param correctGroups the list of correct groups as sets
   * @return true if the proposal matches a correct group
   */
  public static boolean isCorrectGroup(Set<String> proposedWords, List<Set<String>> correctGroups) {
    return correctGroups.contains(proposedWords);
  }

  /**
   * Checks if a proposed set of words is a correct group based on word groups.
   *
   * @param words the proposed words
   * @param groups the list of correct word groups
   * @return true if correct
   */
  public static boolean isCorrectProposal(Set<String> words, List<WordGroup> groups) {
    return isCorrectGroup(words, correctGroupsAsSets(groups));
  }

  /**
   * Returns all words of the game shuffled deterministically based on game ID.
   *
   * @param game the game word groups
   * @param gameId the game ID used as seed
   * @return a shuffled list of all words
   */
  public static List<String> shuffledWords(GameWordGroups game, long gameId) {
    List<String> allWords = game.groups().stream().flatMap(g -> g.words().stream()).toList();
    List<String> shuffled = new ArrayList<>(allWords);
    Collections.shuffle(shuffled, new Random(gameId));
    return List.copyOf(shuffled);
  }
}
