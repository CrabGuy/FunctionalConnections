package server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class User {
    public String username;
    public String passwordHash;
    public int currentStreak = 0;
    public int maxStreak = 0;
    public Map<Long, GameResult> games = new ConcurrentHashMap<>();

    public record GameResult(int mistakes, int rightGuesses) {}

    @JsonCreator
    public User(
            @JsonProperty("username") String username,
            @JsonProperty("passwordHash") String passwordHash
    ) {
        this.username = username;
        this.passwordHash = passwordHash;
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

    public void recalculateStreaks() {
        int tempCurrent = 0;
        int tempMax = 0;
        var sortedGames = games.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();
        for (GameResult game : sortedGames) {
            if (game.rightGuesses() == 4) {
                tempCurrent++;
                tempMax = Math.max(tempMax, tempCurrent);
            } else {
                tempCurrent = 0;
            }
        }
        this.currentStreak = tempCurrent;
        this.maxStreak = Math.max(this.maxStreak, tempMax);
    }
}