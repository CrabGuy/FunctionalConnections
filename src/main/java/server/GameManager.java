package server;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

//TODO: wire game manager and user manager to the server and to show an API for the client.
// Also remember to send the UDP message to the client when time expires
// Extract the saving/loading of a hashmap to/from a json file and apply it to both

public final class GameManager {

    public record WordGroup(String category, Set<String> words) {}

    public record PlayerProgress(int solvedCount, int mistakesMade) {}

    public record Game(
        long id,
        List<WordGroup> wordGroups,
        Duration duration,
        int maxMistakesAllowed,
        Map<String, PlayerProgress> playerStates
    ) {}

    public enum Status { IN_PROGRESS, WON, LOST }

    private final ConcurrentHashMap<Long, Game> games = new ConcurrentHashMap<>();
    private final List<List<WordGroup>> puzzleBank;
    private final Duration gameDuration;
    private final int maxMistakesAllowed;

    public GameManager(List<List<WordGroup>> puzzleBank, Duration gameDuration, int maxMistakesAllowed) {
        this.puzzleBank = puzzleBank;
        this.gameDuration = gameDuration;
        this.maxMistakesAllowed = maxMistakesAllowed;
    }

    public long getCurrentGameId() {
        return Instant.now().toEpochMilli() / gameDuration.toMillis();
    }

    public Game getOrCreateGame(long gameId) {
        return games.computeIfAbsent(gameId, id -> {
            int puzzleIndex = Math.floorMod(id, puzzleBank.size());
            List<WordGroup> groups = puzzleBank.get(puzzleIndex);
            return new Game(id, groups, gameDuration, maxMistakesAllowed, Map.of());
        });
    }

    public Game getActiveGame() {
        return getOrCreateGame(getCurrentGameId());
    }

    public Optional<Game> getGame(long gameId) {
        return Optional.ofNullable(games.get(gameId));
    }

    public Optional<Game> processGuess(long gameId, String player, Set<String> guess) {
        return Optional.ofNullable(
            games.computeIfPresent(gameId, (id, game) -> updateGame(game, player, guess))
        );
    }

    public Status getPlayerStatus(Game game, String player) {
        var progress = game.playerStates().getOrDefault(player, new PlayerProgress(0, 0));
        if (progress.solvedCount() == game.wordGroups().size()) return Status.WON;
        if (progress.mistakesMade() >= game.maxMistakesAllowed()) return Status.LOST;
        return Status.IN_PROGRESS;
    }

    public Duration getRemainingTime(Game game) {
        long currentMs = Instant.now().toEpochMilli();
        long windowStartMs = game.id() * game.duration().toMillis();
        long elapsedMs = currentMs - windowStartMs;
        long remainingMs = game.duration().toMillis() - elapsedMs;
        return remainingMs <= 0 ? Duration.ZERO : Duration.ofMillis(remainingMs);
    }

    private Game updateGame(Game game, String player, Set<String> guess) {
        if (getRemainingTime(game).isZero() || getPlayerStatus(game, player) != Status.IN_PROGRESS) {
            return game;
        }

        var current = game.playerStates().getOrDefault(player, new PlayerProgress(0, 0));
        boolean isCorrect = game.wordGroups().stream().anyMatch(group -> group.words().equals(guess));

        var updated = isCorrect 
            ? new PlayerProgress(current.solvedCount() + 1, current.mistakesMade())
            : new PlayerProgress(current.solvedCount(), current.mistakesMade() + 1);

        var newStates = new HashMap<>(game.playerStates());
        newStates.put(player, updated);

        return new Game(
            game.id(),
            game.wordGroups(),
            game.duration(),
            game.maxMistakesAllowed(),
            Map.copyOf(newStates)
        );
    }
}