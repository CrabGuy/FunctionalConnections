package server;

import java.util.ArrayList;
import java.util.List;

public class User {
    public record GameResult(int mistakes, int rightGuesses) {}

    public String username;
    public String passwordHash;
    public int currentStreak;
    public int maxStreak;
    public List<GameResult> games;

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.games = new ArrayList<>();
    }

    public int getWins() {
        return (int) games.stream()
            .filter(g -> g.mistakes() < 4)
            .count();
    }
}