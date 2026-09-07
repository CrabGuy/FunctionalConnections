package server.game;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import server.dto.PlayerGame;
import server.dto.PlayerGameKey;

/**
 * In-memory implementation of {@link PlayerGameRepository} with thread-safe operations and per-user
 * locking for atomic updates.
 */
public final class InMemoryPlayerGameRepository implements PlayerGameRepository {

  private final ConcurrentHashMap<PlayerGameKey, PlayerGame> store = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Object> userLocks = new ConcurrentHashMap<>();

  /**
   * Returns the lock object for a given username.
   *
   * @param username the username
   * @return the lock object
   */
  private Object lockFor(String username) {
    return userLocks.computeIfAbsent(username, k -> new Object());
  }

  /** {@inheritDoc} */
  @Override
  public PlayerGame findOrCreate(String username, long gameId) {
    Object lock = lockFor(username);
    synchronized (lock) {
      PlayerGameKey key = new PlayerGameKey(username, gameId);
      return store.computeIfAbsent(key, k -> new PlayerGame(username, gameId, List.of()));
    }
  }

  /** {@inheritDoc} */
  @Override
  public void save(PlayerGame playerGame) {
    Object lock = lockFor(playerGame.username());
    synchronized (lock) {
      PlayerGameKey key = new PlayerGameKey(playerGame.username(), playerGame.gameId());
      store.put(key, playerGame);
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<PlayerGame> findByGame(long gameId) {
    return store.values().stream().filter(pg -> pg.gameId() == gameId).toList();
  }

  /** {@inheritDoc} */
  @Override
  public List<PlayerGame> findPlayerGameByUsername(String username) {
    return store.values().stream().filter(pg -> pg.username().equals(username)).toList();
  }

  /** {@inheritDoc} */
  @Override
  public Optional<PlayerGame> findByUsernameAndGame(String username, long gameId) {
    PlayerGameKey key = new PlayerGameKey(username, gameId);
    return Optional.ofNullable(store.get(key));
  }

  /** {@inheritDoc} */
  @Override
  public Set<String> findAllUsernames() {
    return store.values().stream()
        .map(PlayerGame::username)
        .collect(Collectors.toUnmodifiableSet());
  }

  /** {@inheritDoc} */
  @Override
  public List<PlayerGame> findAll() {
    return List.copyOf(store.values());
  }

  /** {@inheritDoc} */
  @Override
  public void updateUsername(String oldUsername, String newUsername) {
    // Lock in a consistent order to avoid deadlocks
    String first = oldUsername.compareTo(newUsername) <= 0 ? oldUsername : newUsername;
    String second = first.equals(oldUsername) ? newUsername : oldUsername;
    Object firstLock = lockFor(first);
    Object secondLock = lockFor(second);

    synchronized (firstLock) {
      synchronized (secondLock) {
        List<PlayerGameKey> keysToMove =
            store.keySet().stream().filter(key -> key.username().equals(oldUsername)).toList();
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
