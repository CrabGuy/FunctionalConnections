package server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class User {
    public final String username;
    public String passwordHash;
    public int currentStreak = 0;
    public int maxStreak = 0;
    public final Map<Long, GameResult> games = new ConcurrentHashMap<>();

    public record GameResult(int mistakes, int rightGuesses) {}

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public int getWins() {
        return (int) games.values().stream()
                .filter(g -> g.rightGuesses() == 4)
                .count();
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
            } else if (game.mistakes() >= 4 || game.rightGuesses() < 4) {
                tempCurrent = 0;
            }
        }

        this.currentStreak = tempCurrent;
        this.maxStreak = Math.max(this.maxStreak, tempMax);
    }
}