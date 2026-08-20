package server;

import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class UserManager {
    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();

    public boolean register(String username, String passwordHash) {
        return users.putIfAbsent(username, new User(username, passwordHash)) == null;
    }

    public boolean authenticate(String username, String passwordHash) {
        User user = users.get(username);
        return user != null && user.passwordHash.equals(passwordHash);
    }

    public boolean updateCredentials(String username, String oldPasswordHash, String newUsername, String newPasswordHash) {
        User user = users.get(username);
        if (user == null || !user.passwordHash.equals(oldPasswordHash)) {
            return false;
        }
        String targetUsername = newUsername == null || newUsername.isBlank() ? username : newUsername;
        if (!targetUsername.equals(username) && users.containsKey(targetUsername)) {
            return false;
        }
        if (!targetUsername.equals(username)) {
            users.remove(username);
            user.username = targetUsername;
            users.put(targetUsername, user);
        }
        if (newPasswordHash != null && !newPasswordHash.isBlank()) {
            user.passwordHash = newPasswordHash;
        }
        return true;
    }

    public User get(String username) {
        return users.get(username);
    }

    public Stream<User> getLeaderboard() {
        return users.values().stream()
                .sorted(Comparator.comparingInt(User::getWins).reversed());
    }

    public int getPosition(String username) {
        return (int) getLeaderboard()
                .takeWhile(user -> !user.username.equals(username))
                .count() + 1;
    }

    public void recordCompletedGame(String username, long gameId, int mistakes, int rightGuesses) {
        User user = users.get(username);
        if (user == null) {
            return;
        }
        user.games.putIfAbsent(gameId, new User.GameResult(mistakes, rightGuesses));
        user.recalculateStreaks();
    }

    public boolean usernameExists(String username) {
        return users.containsKey(username);
    }
}