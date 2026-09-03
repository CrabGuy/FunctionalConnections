package server.stats;

import server.dto.PlayerGame;
import server.dto.Proposal;

import java.util.List;
import java.util.Set;

/**
 * Pure functions for computing score, outcome and mistakes from a PlayerGame.
 */
public final class ScoreCalculator {

    private ScoreCalculator() {}

    /**
     * Returns the number of correct proposals and the number of wrong proposals.
     */
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

    /**
     * Computes the score for a player game using the formula: correct*6 - wrong*4.
     */
    public static int score(PlayerGame playerGame, List<Set<String>> correctGroups) {
        CorrectWrongCount counts = countCorrectWrong(playerGame, correctGroups);
        return counts.correct() * 6 - counts.wrong() * 4;
    }

    /**
     * Determines whether the player has won, lost or the game is still ongoing for them.
     *
     * @param gameCompleted whether the global game has expired
     * @return an {@link Outcome} enum
     */
    public static Outcome outcome(PlayerGame playerGame, List<Set<String>> correctGroups, boolean gameCompleted) {
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