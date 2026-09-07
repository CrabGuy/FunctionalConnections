package shared.game;

public final class GameRules {
  private GameRules() {}

  public static final int CORRECT_TO_WIN = 3;
  public static final int WRONG_TO_LOSE = 4;
  public static final int SCORE_PER_CORRECT = 6;
  public static final int SCORE_PENALTY_PER_WRONG = 4;

  public static boolean isWon(int correctCount) {
    return correctCount >= CORRECT_TO_WIN;
  }

  public static boolean isLost(int wrongCount) {
    return wrongCount >= WRONG_TO_LOSE;
  }

  public static int score(int correct, int wrong) {
    return correct * SCORE_PER_CORRECT - wrong * SCORE_PENALTY_PER_WRONG;
  }
}
