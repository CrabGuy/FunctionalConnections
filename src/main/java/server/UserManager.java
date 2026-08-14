package server;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserManager {
    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();

    public boolean register(String username, String passwordHash) {
        return users.putIfAbsent(username, new User(username, passwordHash)) == null;
    }

    public boolean authenticate(String username, String passwordHash) {
        return Optional.ofNullable(users.get(username))
                .filter(u -> u.passwordHash.equals(passwordHash))
                .isPresent();
    }

    public boolean updateCredentials(String username, String oldPasswordHash, String newUsername, String newPasswordHash) {
        User user = users.get(username);
        if (user == null || !user.passwordHash.equals(oldPasswordHash)) return false;

        boolean renaming = newUsername != null && !newUsername.equals(username);
        if (renaming && users.containsKey(newUsername)) return false;

        if (renaming) {
            users.remove(username);
            users.put(newUsername, user);
        }

        Optional.ofNullable(newPasswordHash).ifPresent(p -> user.passwordHash = p);
        return true;
    }

    public User get(String username) {
        return users.get(username);
    }

    public Stream<User> getLeaderboard() {
        return users.values().stream()
                .sorted(Comparator.comparingInt(User::getWins).reversed());
    }

    public List<User> getTopK(int k) {
        return getLeaderboard()
                .limit(k)
                .collect(Collectors.toList());
    }

    public int getPosition(String username) {
        return (int) getLeaderboard()
                .takeWhile(u -> !u.username.equals(username))
                .count() + 1;
    }

    public void updateStats(String username, long gameId, int mistakes, int rightGuesses) {
        Optional.ofNullable(users.get(username)).ifPresent(user -> {
            user.games.put(gameId, new User.GameResult(mistakes, rightGuesses));
            user.recalculateStreaks();
        });
    }

    public boolean usernameExists(String username) {
        return users.containsKey(username);
    }
}