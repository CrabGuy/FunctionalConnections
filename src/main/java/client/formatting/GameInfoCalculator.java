package client.formatting;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import shared.dto.GameInfoData;
import shared.game.GameRules;

/**
 * Provides helper methods to compute derived information from {@link GameInfoData} such as counts,
 * status, remaining words, and proposal evaluation.
 */
public final class GameInfoCalculator {

  /** Possible outcomes when evaluating a submitted proposal against the current game state. */
  public enum ProposalOutcome {
    /** The proposal matches a correct group. */
    CORRECT,
    /** The proposal matches a previously submitted wrong group. */
    WRONG,
    /** The proposal does not match any existing guesses. */
    UNCHANGED
  }

  private GameInfoCalculator() {
    // Prevent instantiation
  }

  /**
   * Returns the number of correct proposals made in the game.
   *
   * @param info the game information
   * @return the count of correct guesses
   */
  public static int correctProposalCount(GameInfoData info) {
    return info.correctGuesses().size();
  }

  /**
   * Returns the number of wrong proposals made in the game.
   *
   * @param info the game information
   * @return the count of wrong guesses
   */
  public static int mistakeCount(GameInfoData info) {
    return info.wrongGuesses().size();
  }

  /**
   * Computes the current score based on correct and wrong guesses.
   *
   * @param info the game information
   * @return the current score
   */
  public static int score(GameInfoData info) {
    return GameRules.score(correctProposalCount(info), mistakeCount(info));
  }

  /**
   * Returns the list of words that have not yet been correctly grouped.
   *
   * @param info the game information
   * @return a list of remaining words
   */
  public static List<String> remainingWords(GameInfoData info) {
    Set<String> grouped =
        info.correctGuesses().stream().flatMap(Set::stream).collect(Collectors.toSet());
    return info.words().stream().filter(word -> !grouped.contains(word)).toList();
  }

  /**
   * Determines the current game status (ACTIVE, WON, LOST, or INCOMPLETE).
   *
   * @param info the game information
   * @param nowMillis the current time in milliseconds
   * @return the status string
   */
  public static String status(GameInfoData info, long nowMillis) {
    if (GameRules.isWon(correctProposalCount(info))) {
      return "WON";
    }
    if (GameRules.isLost(mistakeCount(info))) {
      return "LOST";
    }
    return info.expiresAt() <= nowMillis ? "INCOMPLETE" : "ACTIVE";
  }

  /**
   * Computes the remaining time in milliseconds before the game expires.
   *
   * @param info the game information
   * @param nowMillis the current time in milliseconds
   * @return the remaining time, never negative
   */
  public static long remainingTimeMillis(GameInfoData info, long nowMillis) {
    return Math.max(0L, info.expiresAt() - nowMillis);
  }

  /**
   * Checks whether the given list of words is already present in the correct or wrong guesses.
   *
   * @param info the game information
   * @param words the words to check
   * @param correct if true, checks correct guesses; otherwise checks wrong guesses
   * @return true if the set of words is already present
   */
  public static boolean containsGuess(GameInfoData info, List<String> words, boolean correct) {
    Set<String> requested = Set.copyOf(words);
    return (correct ? info.correctGuesses() : info.wrongGuesses())
        .stream().anyMatch(requested::equals);
  }

  /**
   * Evaluates the outcome of a submitted proposal by comparing it to existing guesses.
   *
   * @param data the game information after the proposal
   * @param submittedWords the words that were submitted
   * @return the proposal outcome
   */
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
