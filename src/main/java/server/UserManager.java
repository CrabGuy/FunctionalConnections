package server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Comparator;
import java.util.Optional;

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
    
    public boolean updateCredentials(String username, String oldPasswordHash, 
                                     String newUsername, String newPasswordHash) {
        User user = users.get(username);
        if (user == null || !user.passwordHash.equals(oldPasswordHash)) return false;
        
        boolean renaming = newUsername != null && !newUsername.equals(username);
        if (renaming && users.containsKey(newUsername)) return false;
        
        if (renaming) {
            users.remove(username);
            user.username = newUsername;
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
    
    public void updateStats(String username, int mistakes, int rightGuesses) {
        Optional.ofNullable(users.get(username)).ifPresent(user -> {
            boolean won = mistakes < 4;
            user.currentStreak = won ? user.currentStreak + 1 : 0;
            user.maxStreak = Math.max(user.maxStreak, user.currentStreak);
            user.games.add(new User.GameResult(mistakes, rightGuesses));
        });
    }
    
    public boolean usernameExists(String username) {
        return users.containsKey(username);
    }
}