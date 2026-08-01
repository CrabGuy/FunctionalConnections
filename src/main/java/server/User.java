package server;

import java.util.stream.IntStream;

class User {
    public String username;
    public String passwordHash;
    public int currentStreak;
    public int maxStreak;
    public int[] gameMistakes;

    public int getWins() {
        int maxMistakes = gameMistakes.length - 1;
        return IntStream.range(0, maxMistakes).map(i -> gameMistakes[i]).sum();
    }

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.gameMistakes = new int[5];
    }
}
