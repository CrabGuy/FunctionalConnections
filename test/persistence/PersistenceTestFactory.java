package test.persistence;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import server.account.AccountRepository;
import server.dto.Account;
import server.dto.PlayerGame;
import server.dto.PlayerGameKey;
import server.game.PlayerGameRepository;
import server.persistence.FilePersistenceService;
import server.persistence.PersistenceService;

/**
 * Factory for creating test instances of the Persistence slice (Slice F). Uses the concrete
 * FilePersistenceService implementation and simple in‑memory repository implementations for testing
 * persistence round‑trips.
 */
public class PersistenceTestFactory {

    public static PersistenceService createPersistenceService(Path storageDirectory) {
        return new FilePersistenceService(storageDirectory);
    }

    public static AccountRepository createAccountRepository() {
        return new InMemoryAccountRepository();
    }

    public static PlayerGameRepository createPlayerGameRepository() {
        return new InMemoryPlayerGameRepository();
    }

    // ---------------------------------------------------------------
    // Test double implementations (thread‑safe but simple for tests)
    // ---------------------------------------------------------------

    private static final class InMemoryAccountRepository implements AccountRepository {
        private final Map<String, Account> accounts = new ConcurrentHashMap<>();

        @Override
        public Optional<Account> findAccountByUsername(String username) {
            return Optional.ofNullable(accounts.get(username));
        }

        @Override
        public void save(Account account) {
            accounts.put(account.username(), account);
        }

        @Override
        public boolean existsByUsername(String username) {
            return accounts.containsKey(username);
        }

        @Override
        public void deleteByUsername(String username) {
            accounts.remove(username);
        }

        @Override
        public List<Account> findAll() {
            return List.copyOf(accounts.values());
        }
    }

    private static final class InMemoryPlayerGameRepository implements PlayerGameRepository {
        private final ConcurrentHashMap<PlayerGameKey, PlayerGame> store = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, Object> userLocks = new ConcurrentHashMap<>();

        private Object lockFor(String username) {
            return userLocks.computeIfAbsent(username, k -> new Object());
        }

        @Override
        public PlayerGame findOrCreate(String username, long gameId) {
            Object lock = lockFor(username);
            synchronized (lock) {
                PlayerGameKey key = new PlayerGameKey(username, gameId);
                return store.computeIfAbsent(key, k -> new PlayerGame(username, gameId, List.of()));
            }
        }

        @Override
        public void save(PlayerGame playerGame) {
            Object lock = lockFor(playerGame.username());
            synchronized (lock) {
                PlayerGameKey key = new PlayerGameKey(playerGame.username(), playerGame.gameId());
                store.put(key, playerGame);
            }
        }

        @Override
        public List<PlayerGame> findByGame(long gameId) {
            return store.values().stream().filter(pg -> pg.gameId() == gameId).toList();
        }

        @Override
        public List<PlayerGame> findPlayerGameByUsername(String username) {
            return store.values().stream().filter(pg -> pg.username().equals(username)).toList();
        }

        @Override
        public Optional<PlayerGame> findByUsernameAndGame(String username, long gameId) {
            PlayerGameKey key = new PlayerGameKey(username, gameId);
            return Optional.ofNullable(store.get(key));
        }

        @Override
        public Set<String> findAllUsernames() {
            return store.values().stream()
                    .map(PlayerGame::username)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
        }

        @Override
        public List<PlayerGame> findAll() {
            return List.copyOf(store.values());
        }

        @Override
        public void updateUsername(String oldUsername, String newUsername) {
            String first = oldUsername.compareTo(newUsername) <= 0 ? oldUsername : newUsername;
            String second = first.equals(oldUsername) ? newUsername : oldUsername;
            Object firstLock = lockFor(first);
            Object secondLock = lockFor(second);
            synchronized (firstLock) {
            synchronized (secondLock) {
                    List<PlayerGameKey> keysToMove = store.keySet().stream()
                            .filter(key -> key.username().equals(oldUsername))
                            .toList();
                    for (PlayerGameKey oldKey : keysToMove) {
                        PlayerGame value = store.remove(oldKey);
                        if (value != null) {
                            store.put(new PlayerGameKey(newUsername, value.gameId()), value);
                        }
                    }
                }
            }
        }
    }
}