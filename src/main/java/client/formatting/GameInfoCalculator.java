package client.formatting;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import shared.dto.GameInfoData;

public final class GameInfoCalculator {
  public enum ProposalOutcome {
    CORRECT,
    WRONG,
    UNCHANGED
  }

  private GameInfoCalculator() {}

  public static int correctProposalCount(GameInfoData info) {
    return info.correctGuesses().size();
  }

  public static int mistakeCount(GameInfoData info) {
    return info.wrongGuesses().size();
  }

  public static int score(GameInfoData info) {
    return correctProposalCount(info) * 6 - mistakeCount(info) * 4;
  }

  public static List<String> remainingWords(GameInfoData info) {
    Set<String> grouped = new LinkedHashSet<>();
    info.correctGuesses().forEach(grouped::addAll);
    List<String> remaining = new ArrayList<>();
    for (String word : info.words()) {
      if (!grouped.contains(word)) {
        remaining.add(word);
      }
    }
    return List.copyOf(remaining);
  }

  public static String status(GameInfoData info, long nowMillis) {
    if (correctProposalCount(info) >= 3) {
      return "WON";
    }
    if (mistakeCount(info) >= 4) {
      return "LOST";
    }
    return info.expiresAt() <= nowMillis ? "INCOMPLETE" : "ACTIVE";
  }

  public static long remainingTimeMillis(GameInfoData info, long nowMillis) {
    return Math.max(0L, info.expiresAt() - nowMillis);
  }

  public static boolean containsGuess(GameInfoData info, List<String> words, boolean correct) {
    Set<String> requested = Set.copyOf(words);
    return (correct ? info.correctGuesses() : info.wrongGuesses())
        .stream().anyMatch(requested::equals);
  }

  public static ProposalOutcome evaluateProposal(GameInfoData data, List<String> submittedWords) {
    if (containsGuess(data, submittedWords, true)) {
      return ProposalOutcome.CORRECT;
    }
    if (containsGuess(data, submittedWords, false)) {
      return ProposalOutcome.WRONG;
    }
    return ProposalOutcome.UNCHANGED;
  }
}
