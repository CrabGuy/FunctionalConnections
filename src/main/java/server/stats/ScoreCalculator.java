package server.stats;

import java.util.List;
import java.util.Set;
import server.dto.PlayerGame;
import server.game.GameLogic;
import shared.game.GameRules;

/** Utility class for computing game outcomes and scores for player game records. */
public final class ScoreCalculator {

  private ScoreCalculator() {
    // Prevent instantiation
  }

  /**
   * Represents the count of correct and wrong proposals.
   *
   * @param correct number of correct proposals
   * @param wrong number of wrong proposals
   */
  public record CorrectWrongCount(int correct, int wrong) {}

  /** The outcome of a game (won, lost, or incomplete). */
  public enum Outcome {
    WON,
    LOST,
    INCOMPLETE
  }

  /**
   * Counts correct and wrong proposals for a player game.
   *
   * @param playerGame the player game record
   * @param correctGroups the correct groups as sets
   * @return the counts
   */
  public static CorrectWrongCount countCorrectWrong(
      PlayerGame playerGame, List<Set<String>> correctGroups) {
    int correct =
        (int)
            playerGame.proposals().stream()
                .filter(proposal -> GameLogic.isCorrectGroup(proposal.words(), correctGroups))
                .count();
    int wrong = playerGame.proposals().size() - correct;
    return new CorrectWrongCount(correct, wrong);
  }

  /**
   * Computes the score for a player game.
   *
   * @param playerGame the player game record
   * @param correctGroups the correct groups as sets
   * @return the score
   */
  public static int score(PlayerGame playerGame, List<Set<String>> correctGroups) {
    CorrectWrongCount counts = countCorrectWrong(playerGame, correctGroups);
    return GameRules.score(counts.correct(), counts.wrong());
  }

  /**
   * Determines the outcome of a game based on counts.
   *
   * @param playerGame the player game record
   * @param correctGroups the correct groups as sets
   * @return the outcome
   */
  public static Outcome outcome(PlayerGame playerGame, List<Set<String>> correctGroups) {
    CorrectWrongCount counts = countCorrectWrong(playerGame, correctGroups);
    if (GameRules.isWon(counts.correct())) {
      return Outcome.WON;
    } else if (GameRules.isLost(counts.wrong())) {
      return Outcome.LOST;
    } else {
      return Outcome.INCOMPLETE;
    }
  }
}
