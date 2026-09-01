package server.game;

import shared.DataContracts;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

public final class PlayerStatsCalculator {
    private final PlayerProgressStore progressStore;
    private final GameRepository gameRepository;
    private final GameClock clock;

    public PlayerStatsCalculator(PlayerProgressStore progressStore, GameRepository gameRepository, GameClock clock) {
        this.progressStore = progressStore;
        this.gameRepository = gameRepository;
        this.clock = clock;
    }

    public DataContracts.PlayerStatsDto calculateStats(String username) {
        Map<Long, PlayerProgress> userProgress = progressStore.allProgressFor(username);
        Instant now = clock.now();

        int completed = 0;
        int wins = 0;
        int currentStreak = 0;
        int maxStreak = 0;
        int perfectPuzzles = 0;
        Map<Integer, Long> mistakeHistogram = new TreeMap<>();

        var sortedGames = userProgress.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();

        for (var entry : sortedGames) {
            long gameId = entry.getKey();
            PlayerProgress progress = entry.getValue();
            GameSession game = gameRepository.getOrCreateGame(gameId);
            GameSession.Status status = game.playerStatus(progress, now);

            if (status != GameSession.Status.IN_PROGRESS) {
                completed++;
                int mistakes = (int) progress.mistakesMade();
                mistakeHistogram.merge(mistakes, 1L, Long::sum);

                if (status == GameSession.Status.WON) {
                    wins++;
                    currentStreak++;
                    maxStreak = Math.max(maxStreak, currentStreak);
                    if (mistakes == 0) {
                        perfectPuzzles++;
                    }
                } else {
                    currentStreak = 0;
                }
            }
        }

        double winRate = completed == 0 ? 0.0 : (wins * 100.0 / completed);
        double lossRate = completed == 0 ? 0.0 : ((completed - wins) * 100.0 / completed);

        return new DataContracts.PlayerStatsDto(
                completed,
                winRate,
                lossRate,
                currentStreak,
                maxStreak,
                perfectPuzzles,
                mistakeHistogram
        );
    }

    public int calculateWins(String username) {
        Map<Long, PlayerProgress> userProgress = progressStore.allProgressFor(username);
        Instant now = clock.now();
        int wins = 0;
        for (var entry : userProgress.entrySet()) {
            GameSession game = gameRepository.getOrCreateGame(entry.getKey());
            if (game.playerStatus(entry.getValue(), now) == GameSession.Status.WON) {
                wins++;
            }
        }
        return wins;
    }
}