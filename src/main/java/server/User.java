package server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public record User(
        String username,
        String passwordHash,
        Map<Long, GameResult> games,
        int currentStreak,
        int maxStreak
) {
    public record GameResult(int mistakes, int rightGuesses) {}

    @JsonCreator
    public User(
            @JsonProperty("username") String username,
            @JsonProperty("passwordHash") String passwordHash,
            @JsonProperty("games") Map<Long, GameResult> games,
            @JsonProperty("currentStreak") int currentStreak,
            @JsonProperty("maxStreak") int maxStreak
    ) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.games = games == null ? Map.of() : Map.copyOf(games);
        int[] streaks = computeStreaks(this.games);
        this.currentStreak = streaks[0];
        this.maxStreak = streaks[1];
    }

    public int getWins() {
        return (int) games.values().stream()
                .filter(game -> game.rightGuesses() == 4)
                .count();
    }

    public int getPerfectPuzzles() {
        return (int) games.values().stream()
                .filter(game -> game.rightGuesses() == 4 && game.mistakes() == 0)
                .count();
    }

    public Map<Integer, Long> getMistakeHistogram() {
        return games.values().stream()
                .collect(Collectors.groupingBy(GameResult::mistakes, TreeMap::new, Collectors.counting()));
    }

    public User withUsername(String newUsername) {
        return new User(newUsername, passwordHash, games, currentStreak, maxStreak);
    }

    public User withPasswordHash(String newPasswordHash) {
        return new User(username, newPasswordHash, games, currentStreak, maxStreak);
    }

    public User withAddedGame(long gameId, GameResult result) {
        Map<Long, GameResult> updated = new HashMap<>(games);
        updated.putIfAbsent(gameId, result);
        return new User(username, passwordHash, updated, 0, 0);
    }

    private static int[] computeStreaks(Map<Long, GameResult> games) {
        int current = 0;
        int best = 0;
        var sorted = games.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();
        for (GameResult game : sorted) {
            if (game.rightGuesses() == 4) {
                current++;
                best = Math.max(best, current);
            } else {
                current = 0;
            }
        }
        return new int[]{current, best};
    }
}