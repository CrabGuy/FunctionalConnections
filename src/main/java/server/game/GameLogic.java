package server.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import server.dto.GameWordGroups;
import server.dto.WordGroup;

public final class GameLogic {
  private GameLogic() {}

  public static List<Set<String>> correctGroupsAsSets(GameWordGroups game) {
    return game.groups().stream().map(group -> Set.copyOf(group.words())).toList();
  }

  public static List<Set<String>> correctGroupsAsSets(List<WordGroup> groups) {
    return groups.stream().map(group -> Set.copyOf(group.words())).toList();
  }

  public static boolean isCorrectGroup(Set<String> proposedWords, List<Set<String>> correctGroups) {
    return correctGroups.contains(proposedWords);
  }

  public static boolean isCorrectProposal(Set<String> words, List<WordGroup> groups) {
    return isCorrectGroup(words, correctGroupsAsSets(groups));
  }

  public static List<String> shuffledWords(GameWordGroups game, long gameId) {
    List<String> allWords = game.groups().stream().flatMap(g -> g.words().stream()).toList();
    List<String> shuffled = new ArrayList<>(allWords);
    Collections.shuffle(shuffled, new Random(gameId));
    return List.copyOf(shuffled);
  }
}
