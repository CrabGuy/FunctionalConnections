package server.game;

import shared.DataContracts;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public final class PlayerGameState {
    private final GameSession game;
    private final PlayerProgress progress;
    private final Instant now;

    public PlayerGameState(GameSession game, PlayerProgress progress, Instant now) {
        this.game = game;
        this.progress = progress;
        this.now = now;
    }

    public GameSession.Status getStatus() {
        return game.playerStatus(progress, now);
    }

    public long getRemainingTimeMs() {
        return game.remainingTime(now).toMillis();
    }

    public long getScore() {
        return progress.score();
    }

    public long getMistakes() {
        return progress.mistakesMade();
    }

    public List<DataContracts.SolvedGroupDto> getSolvedGroups() {
        return progress.history().stream()
                .filter(PlayerProgress.Guess::isCorrect)
                .map(g -> {
                    String category = game.wordGroups().stream()
                            .filter(wg -> wg.words().equals(g.words()))
                            .map(GameSession.WordGroup::category)
                            .findFirst()
                            .orElse("Unknown");
                    return new DataContracts.SolvedGroupDto(category, g.words());
                })
                .toList();
    }

    public List<String> getRemainingWords() {
        Set<String> solvedWords = progress.history().stream()
                .filter(PlayerProgress.Guess::isCorrect)
                .flatMap(g -> g.words().stream())
                .collect(Collectors.toSet());
        List<String> remaining = game.wordGroups().stream()
                .flatMap(g -> g.words().stream())
                .distinct()
                .filter(w -> !solvedWords.contains(w.toUpperCase()))
                .collect(Collectors.toList());
        Collections.shuffle(remaining, new Random(game.id()));
        return remaining;
    }

    public List<DataContracts.GameGroupDto> getAllGroups() {
        return game.wordGroups().stream()
                .map(g -> new DataContracts.GameGroupDto(g.category(), g.words()))
                .toList();
    }
}