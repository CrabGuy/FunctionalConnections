package shared.game;

/** Contains the rules and scoring constants for the game. */
public final class GameRules {

  private GameRules() {
    // Prevent instantiation
  }

  /** Number of correct groups needed to win. */
  public static final int CORRECT_TO_WIN = 3;

  /** Number of wrong guesses allowed before losing. */
  public static final int WRONG_TO_LOSE = 4;

  /** Score awarded per correct group. */
  public static final int SCORE_PER_CORRECT = 6;

  /** Score penalty per wrong guess. */
  public static final int SCORE_PENALTY_PER_WRONG = 4;

  /**
   * Checks if the game is won based on the number of correct groups.
   *
   * @param correctCount number of correct groups found
   * @return true if won
   */
  public static boolean isWon(int correctCount) {
    return correctCount >= CORRECT_TO_WIN;
  }

  /**
   * Checks if the game is lost based on the number of wrong guesses.
   *
   * @param wrongCount number of wrong guesses
   * @return true if lost
   */
  public static boolean isLost(int wrongCount) {
    return wrongCount >= WRONG_TO_LOSE;
  }

  /**
   * Computes the score based on correct and wrong counts.
   *
   * @param correct number of correct groups
   * @param wrong number of wrong guesses
   * @return the score
   */
  public static int score(int correct, int wrong) {
    return correct * SCORE_PER_CORRECT - wrong * SCORE_PENALTY_PER_WRONG;
  }
}
