package server.game;

import shared.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class GameRepository {
    public record SubmitGuessResult(Optional<PlayerProgress> progress, ErrorCode error) {}

    private static final class ValidationException extends RuntimeException {
        final ErrorCode errorCode;
        ValidationException(ErrorCode errorCode) {
            this.errorCode = errorCode;
        }
    }

    private final ConcurrentHashMap<Long, GameSession> games = new ConcurrentHashMap<>();
    private final PlayerProgressStore progressStore;
    private final PuzzleBank puzzleBank;
    private final GameClock clock;
    private final Duration gameDuration;
    private final int maxMistakesAllowed;
    private volatile long lastGameId = -1;

    public GameRepository(PuzzleBank puzzleBank, GameClock clock, PlayerProgressStore progressStore,
                          int maxMistakesAllowed, Duration gameDuration) {
        this.progressStore = progressStore;
        this.puzzleBank = puzzleBank;
        this.clock = clock;
        this.gameDuration = gameDuration;
        this.maxMistakesAllowed = maxMistakesAllowed;
    }

    public GameSession getOrCreateGame(long gameId) {
        return games.computeIfAbsent(gameId, id -> {
            List<GameSession.WordGroup> groups = puzzleBank.getPuzzleForGameId(id);
            Instant startTime = clock.startTimeForGameId(id);
            return new GameSession(id, groups, startTime, gameDuration, maxMistakesAllowed);
        });
    }

    public GameSession getActiveGame() {
        long currentGameId = clock.currentGameId();
        if (lastGameId != currentGameId) {
            lastGameId = currentGameId;
        }
        return getOrCreateGame(currentGameId);
    }

    public Optional<GameSession> findGame(Long gameId) {
        if (gameId == null) {
            return Optional.of(getActiveGame());
        }
        return Optional.ofNullable(games.get(gameId));
    }

    public Optional<PlayerProgress> getProgress(long gameId, String username) {
        return progressStore.get(gameId, username);
    }

    public Set<String> participantsFor(long gameId) {
        return progressStore.participantsFor(gameId);
    }

    public void ensureParticipation(String username, long gameId) {
        progressStore.compute(gameId, username, (u, existing) -> {
            if (existing == null) {
                return new PlayerProgress(List.of());
            }
            return existing;
        });
    }

    public SubmitGuessResult submitGuess(long gameId, String username, Set<String> words) {
        GameSession game = games.get(gameId);
        if (game == null) {
            return new SubmitGuessResult(Optional.empty(), ErrorCode.GAME_NOT_FOUND);
        }
        Instant now = clock.now();
        try {
            PlayerProgress result = progressStore.compute(gameId, username, (u, existing) -> {
                PlayerProgress current = existing == null ? new PlayerProgress(List.of()) : existing;

                Optional<ErrorCode> validationError = validateGuess(current, words, game, now);
                if (validationError.isPresent()) {
                    throw new ValidationException(validationError.get());
                }

                boolean isCorrect = game.wordGroups().stream().anyMatch(group -> group.words().equals(words));
                List<PlayerProgress.Guess> updatedHistory = new ArrayList<>(current.history());
                updatedHistory.add(new PlayerProgress.Guess(Set.copyOf(words), isCorrect));
                return new PlayerProgress(updatedHistory);
            });
            return new SubmitGuessResult(Optional.of(result), null);
        } catch (ValidationException e) {
            return new SubmitGuessResult(Optional.empty(), e.errorCode);
        }
    }

    private static Optional<ErrorCode> validateGuess(PlayerProgress current, Set<String> words, GameSession game, Instant now) {
        if (game.playerStatus(current, now) != GameSession.Status.IN_PROGRESS) {
            return Optional.of(ErrorCode.GAME_ALREADY_OVER);
        }
        if (!Collections.disjoint(words, current.solvedWords())) {
            return Optional.of(ErrorCode.WORD_ALREADY_SOLVED);
        }
        if (current.containsGuess(words)) {
            return Optional.of(ErrorCode.DUPLICATE_PROPOSAL);
        }
        return Optional.empty();
    }
}