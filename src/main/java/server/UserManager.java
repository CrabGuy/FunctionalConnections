package server;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class UserManager {
    public enum UpdateResult {
        SUCCESS,
        INVALID_CREDENTIALS,
        TARGET_USERNAME_TAKEN
    }

    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();

    public boolean register(String username, String password) {
        String hashed = PasswordHasher.hash(password);
        User newUser = new User(username, hashed);
        return users.putIfAbsent(username, newUser) == null;
    }

    public boolean authenticate(String username, String password) {
        User user = users.get(username);
        return user != null && PasswordHasher.verify(password, user.passwordHash());
    }

    public UpdateResult updateCredentials(String username, String oldPassword,
                                          String newUsername, String newPassword) {
        if (username == null || username.isBlank()) {
            return UpdateResult.INVALID_CREDENTIALS;
        }
        String targetUsername = (newUsername == null || newUsername.isBlank()) ? username : newUsername;
        AtomicReference<UpdateResult> result = new AtomicReference<>(UpdateResult.INVALID_CREDENTIALS);

        if (targetUsername.equals(username)) {
            users.computeIfPresent(username, (name, existing) -> {
                if (!PasswordHasher.verify(oldPassword, existing.passwordHash())) {
                    result.set(UpdateResult.INVALID_CREDENTIALS);
                    return existing;
                }
                String newPasswordHash = (newPassword != null && !newPassword.isBlank())
                        ? PasswordHasher.hash(newPassword)
                        : existing.passwordHash();
                result.set(UpdateResult.SUCCESS);
                return existing.withPasswordHash(newPasswordHash);
            });
            return result.get();
        } else {
            users.compute(username, (name, existing) -> {
                if (existing == null || !PasswordHasher.verify(oldPassword, existing.passwordHash())) {
                    result.set(UpdateResult.INVALID_CREDENTIALS);
                    return existing;
                }
                String newPasswordHash = (newPassword != null && !newPassword.isBlank())
                        ? PasswordHasher.hash(newPassword)
                        : existing.passwordHash();
                User newUser = new User(targetUsername, newPasswordHash);
                if (users.putIfAbsent(targetUsername, newUser) != null) {
                    result.set(UpdateResult.TARGET_USERNAME_TAKEN);
                    return existing;
                }
                result.set(UpdateResult.SUCCESS);
                return null;
            });
            return result.get();
        }
    }

    public User get(String username) {
        return users.get(username);
    }

    public boolean usernameExists(String username) {
        return users.containsKey(username);
    }

    public Set<String> getAllUsernames() {
        return Set.copyOf(users.keySet());
    }

    public Map<String, User> snapshot() {
        return Map.copyOf(users);
    }

    public void loadSnapshot(Map<String, User> snapshot) {
        users.clear();
        users.putAll(snapshot);
    }
}