package server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import shared.JsonCodec;

public class UserManager {
    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
    
    public boolean register(String username, String passwordHash) {
        if (users.containsKey(username)) return false;
        users.put(username, new User(username, passwordHash));
        return true;
    }
    
    public boolean authenticate(String username, String passwordHash) {
        User user = users.get(username);
        return user != null && user.passwordHash.equals(passwordHash);
    }
    
    public boolean updateCredentials(String username, String oldPasswordHash, 
                                     String newUsername, String newPasswordHash) {
        User user = users.get(username);
        if (user == null || !user.passwordHash.equals(oldPasswordHash)) return false;
        
        if (newUsername != null && !newUsername.equals(username) && users.containsKey(newUsername)) 
            return false;
        
        if (newUsername != null && !newUsername.equals(username)) {
            users.remove(username);
            user.username = newUsername;
            users.put(newUsername, user);
        }
        
        if (newPasswordHash != null) user.passwordHash = newPasswordHash;
        
        return true;
    }
    
    public User get(String username) {
        return users.get(username);
    }
    
    public List<User> getLeaderboard() {
        return users.values().stream()
            .sorted(Comparator
                .comparingInt((User u) -> u.getWins()).reversed())
            .collect(Collectors.toList());
    }
    
    public List<User> getTopK(int k) {
        return getLeaderboard().stream().limit(k).collect(Collectors.toList());
    }
    
    public int getPosition(String username) {
        List<User> board = getLeaderboard();
        return (int) board.stream()
            .takeWhile(u -> !u.username.equals(username))
            .count() + 1;
    }
    
    public void updateStats(String username, int mistakes) {
        User user = users.get(username);
        if (user == null) return;

        boolean won = mistakes < user.gameMistakes.length - 1;

        user.currentStreak = won ? user.currentStreak + 1 : 0;
        user.maxStreak = Math.max(user.maxStreak, user.currentStreak);

        user.gameMistakes[mistakes]++;
    }
    
    public boolean usernameExists(String username) {
        return users.containsKey(username);
    }
    
    public void saveToFile(String filePath) throws IOException {
        String json = JsonCodec.serialize(users);
        Files.write(Paths.get(filePath), json.getBytes());
    }
    
    public void loadFromFile(String filePath) throws IOException {
        String json = Files.readString(Paths.get(filePath));
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, User> loaded = JsonCodec.deserialize(json, ConcurrentHashMap.class);
        users.putAll(loaded);
    }
}