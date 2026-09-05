package server.stats;

import server.dto.PlayerGame;
import server.dto.Proposal;

import java.util.List;
import java.util.Set;

public final class ScoreCalculator {
    private ScoreCalculator() {}

    public static CorrectWrongCount countCorrectWrong(PlayerGame playerGame, List<Set<String>> correctGroups) {
        int correct = 0;
        for (Proposal proposal : playerGame.proposals()) {
            if (correctGroups.contains(proposal.words())) {
                correct++;
            }
        }
        int wrong = playerGame.proposals().size() - correct;
        return new CorrectWrongCount(correct, wrong);
    }

    public static int score(PlayerGame playerGame, List<Set<String>> correctGroups) {
        CorrectWrongCount counts = countCorrectWrong(playerGame, correctGroups);
        return counts.correct() * 6 - counts.wrong() * 4;
    }

    public static Outcome outcome(PlayerGame playerGame, List<Set<String>> correctGroups) {
        CorrectWrongCount counts = countCorrectWrong(playerGame, correctGroups);
        boolean won = counts.correct() >= 3;
        boolean lost = counts.wrong() >= 4;
        if (won) {
            return Outcome.WON;
        } else if (lost) {
            return Outcome.LOST;
        } else {
            return Outcome.INCOMPLETE;
        }
    }

    public record CorrectWrongCount(int correct, int wrong) {}
    public enum Outcome { WON, LOST, INCOMPLETE }
}