package server.game;

import server.storage.MapStorage;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class GameRepository {
    private final ConcurrentHashMap<Long, GameSession> games = new ConcurrentHashMap<>();
    private final PlayerProgressStore progressStore;
    private final PuzzleBank puzzleBank;
    private final GameClock clock;
    private final int maxInMemoryGames;
    private final Path archiveDir;
    private final Duration gameDuration;
    private final int maxMistakesAllowed;

    public GameRepository(PuzzleBank puzzleBank, GameClock clock, int maxInMemoryGames,
                          Path storageDir, int maxMistakesAllowed, Duration gameDuration) {
        this.progressStore = new PlayerProgressStore();
        this.puzzleBank = puzzleBank;
        this.clock = clock;
        this.maxInMemoryGames = maxInMemoryGames;
        this.archiveDir = storageDir.resolve("archive");
        this.gameDuration = gameDuration;
        this.maxMistakesAllowed = maxMistakesAllowed;
    }

    public GameSession getOrCreateGame(long gameId) {
        GameSession game = games.computeIfAbsent(gameId, id -> {
            List<GameSession.WordGroup> groups = puzzleBank.getPuzzleForGameId(id);
            Instant startTime = clock.startTimeForGameId(id);
            return new GameSession(id, groups, startTime, gameDuration, maxMistakesAllowed);
        });
        evictIfNeeded();
        return game;
    }

    public GameSession getActiveGame() {
        return getOrCreateGame(clock.currentGameId());
    }

    public Optional<GameSession> findGame(Long gameId) {
        if (gameId == null) {
            return Optional.of(getActiveGame());
        }
        GameSession game = games.get(gameId);
        if (game != null) {
            return Optional.of(game);
        }
        return loadFromArchive(gameId);
    }

    private Optional<GameSession> loadFromArchive(long gameId) {
        Path archiveFile = archiveDir.resolve("game_" + gameId + ".json");
        if (!java.nio.file.Files.exists(archiveFile)) {
            return Optional.empty();
        }
        try {
            GameSession loaded = MapStorage.load(archiveFile, GameSession.class);
            return Optional.ofNullable(loaded);
        } catch (Exception e) {
            System.err.println("Failed to load archived game " + gameId + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    private void evictIfNeeded() {
        while (games.size() > maxInMemoryGames) {
            Long oldestId = games.keySet().stream().min(Long::compare).orElse(null);
            if (oldestId == null) break;
            GameSession removed = games.remove(oldestId);
            if (removed != null) {
                archiveGame(removed);
                progressStore.removeGame(oldestId);
            }
        }
    }

    private void archiveGame(GameSession game) {
        try {
            java.nio.file.Files.createDirectories(archiveDir);
            Path archiveFile = archiveDir.resolve("game_" + game.id() + ".json");
            MapStorage.save(archiveFile, game);
        } catch (Exception e) {
            System.err.println("Failed to archive game " + game.id() + ": " + e.getMessage());
        }
    }

    public Optional<PlayerProgress> getProgress(long gameId, String username) {
        return progressStore.get(gameId, username);
    }

    public Set<String> participantsFor(long gameId) {
        return progressStore.participantsFor(gameId);
    }

    public Optional<PlayerProgress> submitGuess(long gameId, String username, Set<String> words) {
        GameSession game = games.get(gameId);
        if (game == null) {
            return Optional.empty();
        }
        Instant now = Instant.now();
        if (game.remainingTime(now).isZero()) {
            return Optional.empty();
        }
        PlayerProgress current = progressStore.get(gameId, username)
                .orElse(new PlayerProgress(List.of()));
        if (current.containsGuess(words) || game.playerStatus(current, now) != GameSession.Status.IN_PROGRESS) {
            return Optional.empty();
        }
        boolean isCorrect = game.wordGroups().stream().anyMatch(group -> group.words().equals(words));
        List<PlayerProgress.Guess> updatedHistory = new ArrayList<>(current.history());
        updatedHistory.add(new PlayerProgress.Guess(Set.copyOf(words), isCorrect));
        PlayerProgress newProgress = new PlayerProgress(updatedHistory);
        progressStore.put(gameId, username, newProgress);
        return Optional.of(newProgress);
    }

    public Map<Long, GameSession> snapshot() {
        return Map.copyOf(games);
    }

    public PlayerProgressStore progressStore() {
        return progressStore;
    }
}