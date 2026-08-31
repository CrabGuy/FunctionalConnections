package server.game;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class GameSession {
    public record WordGroup(String category, Set<String> words) {}

    public enum Status {
        IN_PROGRESS, WON, LOST
    }

    private final long id;
    private final List<WordGroup> wordGroups;
    private final Instant startTime;
    private final Duration duration;
    private final int maxMistakesAllowed;

    public GameSession(long id, List<WordGroup> wordGroups, Instant startTime, Duration duration, int maxMistakesAllowed) {
        this.id = id;
        this.wordGroups = List.copyOf(wordGroups);
        this.startTime = startTime;
        this.duration = duration;
        this.maxMistakesAllowed = maxMistakesAllowed;
    }

    public long id() {
        return id;
    }

    public List<WordGroup> wordGroups() {
        return wordGroups;
    }

    public Instant startTime() {
        return startTime;
    }

    public Duration duration() {
        return duration;
    }

    public int maxMistakesAllowed() {
        return maxMistakesAllowed;
    }

    public Set<String> allWords() {
        return wordGroups.stream()
                .flatMap(group -> group.words().stream())
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }

    public Duration remainingTime(Instant now) {
        Duration elapsed = Duration.between(startTime, now);
        Duration remaining = duration.minus(elapsed);
        return remaining.isNegative() || remaining.isZero() ? Duration.ZERO : remaining;
    }

    public Status playerStatus(PlayerProgress progress, Instant now) {
        if (progress.solvedCount() == wordGroups.size()) return Status.WON;
        if (remainingTime(now).isZero() || progress.mistakesMade() >= maxMistakesAllowed)
            return Status.LOST;
        return Status.IN_PROGRESS;
    }
}