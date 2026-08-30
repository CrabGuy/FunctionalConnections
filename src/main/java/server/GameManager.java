package server;

import com.fasterxml.jackson.databind.type.TypeFactory;
import shared.JsonCodec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class GameManager {

    public record WordGroup(String category, Set<String> words) {}

    public record Guess(Set<String> words, boolean isCorrect) {}

    public record PlayerProgress(List<Guess> history) {
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

    public record Game(
            long id,
            List<WordGroup> wordGroups,
            Instant startTime,
            Duration duration,
            int maxMistakesAllowed
    ) {
        public Game {
            wordGroups = List.copyOf(wordGroups);
        }

        public Set<String> allWords() {
            return wordGroups.stream()
                    .flatMap(group -> group.words().stream())
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());
        }

        public Duration remainingTime() {
            Duration elapsed = Duration.between(startTime, Instant.now());
            Duration remaining = duration.minus(elapsed);
            return remaining.isNegative() || remaining.isZero() ? Duration.ZERO : remaining;
        }

        public Status playerStatus(PlayerProgress progress) {
            if (progress.solvedCount() == wordGroups.size()) return Status.WON;
            if (remainingTime().isZero() || progress.mistakesMade() >= maxMistakesAllowed)
                return Status.LOST;
            return Status.IN_PROGRESS;
        }
    }

    public enum Status { IN_PROGRESS, WON, LOST }

    private final ConcurrentHashMap<Long, Game> games = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, PlayerProgress>> playerProgressByGame =
            new ConcurrentHashMap<>();

    private final List<List<WordGroup>> puzzleBank;
    private final Duration gameDuration;
    private final int maxMistakesAllowed;

    public GameManager(String filePath, Duration gameDuration, int maxMistakesAllowed) {
        this.puzzleBank = loadPuzzleBank(filePath);
        this.gameDuration = gameDuration;
        this.maxMistakesAllowed = maxMistakesAllowed;
    }

    private static List<List<WordGroup>> loadPuzzleBank(String filePath) {
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            throw new RuntimeException("Puzzle bank file not found at " + path.toAbsolutePath()
                    + ". Place a valid puzzle data file there before starting the server.");
        }
        try {
            String jsonContent = Files.readString(path);
            var typeFactory = TypeFactory.defaultInstance();
            var type = typeFactory.constructCollectionType(List.class, GameDataDto.class);
            List<GameDataDto> rawGames = JsonCodec.deserialize(jsonContent, type);
            if (rawGames == null || rawGames.isEmpty()) {
                throw new RuntimeException("Puzzle bank file at " + path.toAbsolutePath() + " contains no games.");
            }
            return rawGames.stream()
                    .map(game -> game.groups().stream()
                            .map(group -> new WordGroup(group.theme(), Set.copyOf(group.words())))
                            .toList())
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load puzzle bank from " + path.toAbsolutePath(), e);
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
            Instant startTime = Instant.ofEpochMilli(id * gameDuration.toMillis());
            return new Game(id, groups, startTime, gameDuration, maxMistakesAllowed);
        });
    }

    public Game getActiveGame() {
        return getOrCreateGame(getCurrentGameId());
    }

    public Optional<Game> resolveGame(Long requestedId) {
        return requestedId == null || requestedId == getCurrentGameId()
                ? Optional.of(getActiveGame())
                : Optional.ofNullable(games.get(requestedId));
    }

    public Optional<PlayerProgress> getPlayerProgress(long gameId, String username) {
        var innerMap = playerProgressByGame.get(gameId);
        return innerMap == null ? Optional.empty() : Optional.ofNullable(innerMap.get(username));
    }

    public Set<String> playersForGame(long gameId) {
        var innerMap = playerProgressByGame.get(gameId);
        return innerMap == null ? Set.of() : Set.copyOf(innerMap.keySet());
    }

    public Optional<PlayerProgress> processGuess(long gameId, String username, Set<String> guess) {
        Game game = games.get(gameId);
        if (game == null || game.remainingTime().isZero()) {
            return Optional.empty();
        }

        var innerMap = playerProgressByGame.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>());
        PlayerProgress current = innerMap.getOrDefault(username, new PlayerProgress(List.of()));

        if (current.containsGuess(guess) || game.playerStatus(current) != Status.IN_PROGRESS) {
            return Optional.empty();
        }

        boolean isCorrect = game.wordGroups().stream().anyMatch(group -> group.words().equals(guess));
        var updatedHistory = new ArrayList<>(current.history());
        updatedHistory.add(new Guess(Set.copyOf(guess), isCorrect));
        var newProgress = new PlayerProgress(updatedHistory);
        innerMap.put(username, newProgress);
        return Optional.of(newProgress);
    }

    public void saveGames(Path path) {
        ConcurrentMapStorage.save(path, games);
    }

    public void savePlayerProgress(Path path) {
        ConcurrentMapStorage.save(path, playerProgressByGame);
    }

    public void loadGames(Path path) {
        ConcurrentHashMap<Long, Game> loaded = ConcurrentMapStorage.load(path, Long.class, Game.class);
        games.clear();
        games.putAll(loaded);
    }

    @SuppressWarnings("unchecked")
    public void loadPlayerProgress(Path path) {
        ConcurrentHashMap<Long, ConcurrentHashMap<String, PlayerProgress>> loaded =
                (ConcurrentHashMap<Long, ConcurrentHashMap<String, PlayerProgress>>) (ConcurrentHashMap<?, ?>)
                        ConcurrentMapStorage.load(path, Long.class, ConcurrentHashMap.class);
        playerProgressByGame.clear();
        playerProgressByGame.putAll(loaded);
    }
}