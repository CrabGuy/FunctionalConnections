package test.stats;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import server.account.AccountService;
import server.account.exceptions.InvalidTokenException;
import server.dto.AccountPrincipal;
import server.dto.GameWordGroups;
import server.dto.PlayerGame;
import server.dto.PlayerGameKey;
import server.game.GameClock;
import server.game.GameRepository;
import server.game.PlayerGameRepository;
import server.game.exceptions.GameNotFoundException;
import server.stats.LeaderboardService;
import server.stats.LeaderboardServiceImpl;
import server.stats.StatsService;
import server.stats.StatsServiceImpl;
import shared.dto.LoginData;
import shared.dto.RegisterData;
import shared.dto.UpdateCredentialsData;

public class StatsTestFactory {

  public static GameClock createGameClock(long gameDurationMillis) {
    return new GameClockImpl(gameDurationMillis);
  }

  public static GameRepository createGameRepository(Map<Long, GameWordGroups> gameGroups) {
    return new StubGameRepository(gameGroups);
  }

  public static PlayerGameRepository createPlayerGameRepository() {
    return new InMemoryPlayerGameRepository();
  }

  public static AccountService createAccountService(Map<String, String> tokenToUsername) {
    return new StubAccountService(tokenToUsername);
  }

  public static StatsService createStatsService(
      AccountService accountService,
      PlayerGameRepository playerGames,
      GameRepository gameRepo,
      GameClock clock) {
    return new StatsServiceImpl(accountService, playerGames, gameRepo, clock);
  }

  public static LeaderboardService createLeaderboardService(
      AccountService accountService, PlayerGameRepository playerGames, GameRepository gameRepo) {
    return new LeaderboardServiceImpl(accountService, playerGames, gameRepo);
  }

  // -------- Private implementations --------

  private static class GameClockImpl implements GameClock {
    private final long duration;

    GameClockImpl(long duration) {
      this.duration = duration;
    }

    @Override
    public long currentGameId(long nowMillis) {
      return nowMillis / duration;
    }

    @Override
    public long startedAt(long gameId) {
      return gameId * duration;
    }

    @Override
    public long expiresAt(long gameId) {
      return (gameId + 1) * duration;
    }

    @Override
    public boolean isCompleted(long gameId, long nowMillis) {
      return nowMillis >= expiresAt(gameId);
    }
  }

  private static class StubGameRepository implements GameRepository {
    private final Map<Long, GameWordGroups> groupsByGame;

    StubGameRepository(Map<Long, GameWordGroups> groupsByGame) {
      this.groupsByGame = new HashMap<>(groupsByGame);
    }

    @Override
    public GameWordGroups loadById(long gameId) throws GameNotFoundException {
      GameWordGroups g = groupsByGame.get(gameId);
      if (g == null) {
        throw new GameNotFoundException(gameId);
      }
      return g;
    }

    @Override
    public boolean exists(long gameId) {
      return groupsByGame.containsKey(gameId);
    }
  }

  private static class InMemoryPlayerGameRepository implements PlayerGameRepository {
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
        store.put(new PlayerGameKey(playerGame.username(), playerGame.gameId()), playerGame);
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
    public Set<String> findAllUsernames() {
      return store.values().stream()
          .map(PlayerGame::username)
          .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override
    public Optional<PlayerGame> findByUsernameAndGame(String username, long gameId) {
      return Optional.ofNullable(store.get(new PlayerGameKey(username, gameId)));
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

  private static class StubAccountService implements AccountService {
    private final Map<String, String> tokenToUsername;

    StubAccountService(Map<String, String> tokenToUsername) {
      this.tokenToUsername = new HashMap<>(tokenToUsername);
    }

    @Override
    public AccountPrincipal resolve(String accountToken) throws InvalidTokenException {
      String username = tokenToUsername.get(accountToken);
      if (username == null) {
        throw new InvalidTokenException("unknown token");
      }
      return new AccountPrincipal(username, Long.MAX_VALUE);
    }

    @Override
    public RegisterData register(String username, String password) {
      throw new UnsupportedOperationException();
    }

    @Override
    public LoginData login(String username, String password, int udpPort, String remoteAddress) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void logout(String accountToken) {
      throw new UnsupportedOperationException();
    }

    @Override
    public UpdateCredentialsData updateCredentials(
        String oldUsername, String newUsername, String oldPassword, String newPassword) {
      throw new UnsupportedOperationException();
    }
  }
}
