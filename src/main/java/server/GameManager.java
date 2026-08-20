//TODO: This is doing too much, separate individual games, game manager, player progress/history
package server;

import com.fasterxml.jackson.databind.type.TypeFactory;
import shared.JsonCodec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class GameManager {
    public record WordGroup(String category, Set<String> words) {}

    public record Guess(Set<String> words, boolean isCorrect) {}

    public record PlayerProgress(List<Guess> history) {
        public PlayerProgress {
            history = List.copyOf(history);
        }

        public long solvedCount() {
            return history.stream().filter(Guess::isCorrect).count();
        }

        public long mistakesMade() {
            return history.stream().filter(guess -> !guess.isCorrect()).count();
        }

        public boolean containsGuess(Set<String> words) {
            return history.stream().anyMatch(guess -> guess.words().equals(words));
        }
    }

    public record Game(
            long id,
            List<WordGroup> wordGroups,
            Duration duration,
            int maxMistakesAllowed,
            Map<String, PlayerProgress> playerStates
    ) {
        public Game {
            wordGroups = List.copyOf(wordGroups);
            playerStates = Map.copyOf(playerStates);
        }
    }

    public enum Status { IN_PROGRESS, WON, LOST }

    private final ConcurrentHashMap<Long, Game> games = new ConcurrentHashMap<>();
    private final List<List<WordGroup>> puzzleBank;
    private final Duration gameDuration;
    private final int maxMistakesAllowed;

    public GameManager(String filePath, Duration gameDuration, int maxMistakesAllowed) {
        this.puzzleBank = loadPuzzleBank(filePath);
        this.gameDuration = gameDuration;
        this.maxMistakesAllowed = maxMistakesAllowed;
    }

    private static List<List<WordGroup>> loadPuzzleBank(String filePath) {
        try {
            String jsonContent = Files.readString(Path.of(filePath));
            var typeFactory = TypeFactory.defaultInstance();
            var type = typeFactory.constructCollectionType(List.class, GameDataDto.class);
            List<GameDataDto> rawGames = JsonCodec.deserialize(jsonContent, type);
            return rawGames.stream()
                    .map(game -> game.groups().stream()
                            .map(group -> new WordGroup(group.theme(), Set.copyOf(group.words())))
                            .toList())
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load puzzle bank from " + filePath, e);
        }
    }

    private record GameDataDto(int gameId, List<WordGroupDto> groups) {}

    private record WordGroupDto(String theme, List<String> words) {}

    public long getCurrentGameId() {
        return Instant.now().toEpochMilli() / gameDuration.toMillis();
    }

    public Game getOrCreateGame(long gameId) {
        return games.computeIfAbsent(gameId, id -> {
            int puzzleIndex = (int) Math.floorMod(id, puzzleBank.size());
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
        var progress = game.playerStates().getOrDefault(player, new PlayerProgress(List.of()));
        if (progress.solvedCount() == game.wordGroups().size()) {
            return Status.WON;
        }
        if (getRemainingTime(game).isZero() || progress.mistakesMade() >= game.maxMistakesAllowed()) {
            return Status.LOST;
        }
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
        var currentProgress = game.playerStates().getOrDefault(player, new PlayerProgress(List.of()));
        if (currentProgress.containsGuess(guess)) {
            return game;
        }
        boolean isCorrect = game.wordGroups().stream().anyMatch(group -> group.words().equals(guess));
        var updatedHistory = new ArrayList<>(currentProgress.history());
        updatedHistory.add(new Guess(Set.copyOf(guess), isCorrect));
        var updatedStates = new HashMap<>(game.playerStates());
        updatedStates.put(player, new PlayerProgress(updatedHistory));
        return new Game(
                game.id(),
                game.wordGroups(),
                game.duration(),
                game.maxMistakesAllowed(),
                updatedStates
        );
    }
}