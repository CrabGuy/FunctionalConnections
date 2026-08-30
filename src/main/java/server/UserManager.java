package server;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class UserManager {
    public enum UpdateResult {
        SUCCESS,
        INVALID_CREDENTIALS,
        TARGET_USERNAME_TAKEN
    }

    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();

    public boolean register(String username, String passwordHash) {
        return users.putIfAbsent(username, new User(username, passwordHash, Map.of(), 0, 0)) == null;
    }

    public boolean authenticate(String username, String passwordHash) {
        User user = users.get(username);
        return user != null && user.passwordHash().equals(passwordHash);
    }

    // Synchronized to ensure atomicity of the multi-step update.
    public synchronized UpdateResult updateCredentials(String username, String oldPasswordHash,
                                                       String newUsername, String newPasswordHash) {
        User user = users.get(username);
        if (user == null || !user.passwordHash().equals(oldPasswordHash)) {
            return UpdateResult.INVALID_CREDENTIALS;
        }

        String targetUsername = newUsername == null || newUsername.isBlank() ? username : newUsername;
        if (!targetUsername.equals(username) && users.containsKey(targetUsername)) {
            return UpdateResult.TARGET_USERNAME_TAKEN;
        }

        User updated = user;
        if (!targetUsername.equals(username)) {
            updated = updated.withUsername(targetUsername);
            users.remove(username);
        }
        if (newPasswordHash != null && !newPasswordHash.isBlank()) {
            updated = updated.withPasswordHash(newPasswordHash);
        }
        users.put(targetUsername, updated);
        return UpdateResult.SUCCESS;
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
                .takeWhile(user -> !user.username().equals(username))
                .count() + 1;
    }

    public void recordCompletedGame(String username, long gameId, int mistakes, int rightGuesses) {
        users.computeIfPresent(username, (name, user) -> {
            User.GameResult result = new User.GameResult(mistakes, rightGuesses);
            return user.withAddedGame(gameId, result);
        });
    }

    public boolean usernameExists(String username) {
        return users.containsKey(username);
    }

    public void save(Path path) {
        ConcurrentMapStorage.save(path, users);
    }

    public void load(Path path) {
        ConcurrentHashMap<String, User> loaded = ConcurrentMapStorage.load(path, String.class, User.class);
        users.clear();
        users.putAll(loaded);
    }
}