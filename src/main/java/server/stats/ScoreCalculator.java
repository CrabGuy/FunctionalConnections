package server.stats;

import java.util.List;
import java.util.Set;
import server.dto.PlayerGame;
import server.game.GameLogic;
import shared.game.GameRules;

public final class ScoreCalculator {

  private ScoreCalculator() {}

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

  public static int score(PlayerGame playerGame, List<Set<String>> correctGroups) {
    CorrectWrongCount counts = countCorrectWrong(playerGame, correctGroups);
    return GameRules.score(counts.correct(), counts.wrong());
  }

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

  public record CorrectWrongCount(int correct, int wrong) {}

  public enum Outcome {
    WON,
    LOST,
    INCOMPLETE
  }
}
