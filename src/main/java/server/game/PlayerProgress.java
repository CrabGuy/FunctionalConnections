package server.game;

import java.util.List;
import java.util.Set;

public record PlayerProgress(List<Guess> history) {
    public record Guess(Set<String> words, boolean isCorrect) {}

    private static final long POINTS_PER_CORRECT_GUESS = 6;
    private static final long POINTS_PER_MISTAKE = 4;

    public PlayerProgress {
        history = List.copyOf(history);
    }

    public long solvedCount() {
        return history.stream().filter(Guess::isCorrect).count();
    }

    public long mistakesMade() {
        return history.stream().filter(guess -> !guess.isCorrect()).count();
    }

    public long score() {
        return POINTS_PER_CORRECT_GUESS * solvedCount() - POINTS_PER_MISTAKE * mistakesMade();
    }

    public boolean containsGuess(Set<String> words) {
        return history.stream().anyMatch(guess -> guess.words().equals(words));
    }
}