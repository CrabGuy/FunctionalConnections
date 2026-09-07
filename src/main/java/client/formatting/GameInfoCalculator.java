package client.formatting;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import shared.dto.GameInfoData;
import shared.game.GameRules;

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
    return GameRules.score(correctProposalCount(info), mistakeCount(info));
  }

  public static List<String> remainingWords(GameInfoData info) {
    Set<String> grouped =
        info.correctGuesses().stream().flatMap(Set::stream).collect(Collectors.toSet());
    return info.words().stream().filter(word -> !grouped.contains(word)).toList();
  }

  public static String status(GameInfoData info, long nowMillis) {
    if (GameRules.isWon(correctProposalCount(info))) {
      return "WON";
    }
    if (GameRules.isLost(mistakeCount(info))) {
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
