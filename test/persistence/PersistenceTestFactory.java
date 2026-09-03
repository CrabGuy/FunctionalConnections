package test.persistence;

import server.account.AccountRepository;
import server.game.PlayerGameRepository;
import server.persistence.PersistenceService;
import server.persistence.FilePersistenceService;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import server.dto.Account;
import server.dto.PlayerGame;

/**
 * Factory for creating test instances of the Persistence slice (Slice F).
 * Uses the concrete FilePersistenceService implementation and simple in‑memory
 * repository implementations for testing persistence round‑trips.
 */
public class PersistenceTestFactory {

    /**
     * Creates a FilePersistenceService that uses the given directory as its storage root.
     * The directory will be created if it does not exist.
     */
    public static PersistenceService createPersistenceService(Path storageDirectory) {
        return new FilePersistenceService(storageDirectory);
    }

    /**
     * Creates an in‑memory AccountRepository for testing.
     */
    public static AccountRepository createAccountRepository() {
        return new InMemoryAccountRepository();
    }

    /**
     * Creates an in‑memory PlayerGameRepository for testing.
     */
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
        private final Map<String, List<PlayerGame>> gamesByUser = new ConcurrentHashMap<>();
        private final Map<Long, List<PlayerGame>> gamesByGameId = new ConcurrentHashMap<>();

        @Override
        public PlayerGame findOrCreate(String username, long gameId) {
            Optional<PlayerGame> existing = findByUsernameAndGame(username, gameId);
            if (existing.isPresent()) {
                return existing.get();
            }
            PlayerGame newGame = new PlayerGame(username, gameId, new ArrayList<>());
            save(newGame);
            return newGame;
        }

        @Override
        public void save(PlayerGame playerGame) {
            // Remove any previous entry for the same (username, gameId)
            findByUsernameAndGame(playerGame.username(), playerGame.gameId())
                    .ifPresent(old -> {
                        gamesByUser.getOrDefault(playerGame.username(), new CopyOnWriteArrayList<>())
                                .remove(old);
                        gamesByGameId.getOrDefault(playerGame.gameId(), new CopyOnWriteArrayList<>())
                                .remove(old);
                    });

            gamesByUser.computeIfAbsent(playerGame.username(), k -> new CopyOnWriteArrayList<>())
                    .add(playerGame);
            gamesByGameId.computeIfAbsent(playerGame.gameId(), k -> new CopyOnWriteArrayList<>())
                    .add(playerGame);
        }

        @Override
        public List<PlayerGame> findByGame(long gameId) {
            return List.copyOf(gamesByGameId.getOrDefault(gameId, new CopyOnWriteArrayList<>()));
        }

        @Override
        public List<PlayerGame> findPlayerGameByUsername(String username) {
            return List.copyOf(gamesByUser.getOrDefault(username, new CopyOnWriteArrayList<>()));
        }

        @Override
        public Set<String> findAllUsernames() {
            return Set.copyOf(gamesByUser.keySet());
        }

        @Override
        public Optional<PlayerGame> findByUsernameAndGame(String username, long gameId) {
            return gamesByUser.getOrDefault(username, new CopyOnWriteArrayList<>())
                    .stream()
                    .filter(pg -> pg.gameId() == gameId)
                    .findFirst();
        }

        @Override
        public List<PlayerGame> findAll() {
            return gamesByUser.values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toUnmodifiableList());
        }
    }
}