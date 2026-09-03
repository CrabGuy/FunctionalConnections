package test.stats;

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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating test instances of the Stats & Leaderboard slice (Slice D).
 * Provides stubs and in-memory implementations for all dependencies.
 * Test time control is achieved by using different {@link GameClock} durations:
 * <ul>
 *   <li>Large duration → games are always active (for ongoing-game tests)</li>
 *   <li>Duration = 1 ms → any game with an id smaller than the current time is completed</li>
 * </ul>
 */
public class StatsTestFactory {

    // -------- GameClock --------

    public static GameClock createGameClock(long gameDurationMillis) {
        return new GameClockImpl(gameDurationMillis);
    }

    // -------- GameRepository (stub) --------

    public static GameRepository createGameRepository(Map<Long, GameWordGroups> gameGroups) {
        return new StubGameRepository(gameGroups);
    }

    // -------- PlayerGameRepository (in-memory) --------

    public static PlayerGameRepository createPlayerGameRepository() {
        return new InMemoryPlayerGameRepository();
    }

    // -------- AccountService (stub) --------

    public static AccountService createAccountService(Map<String, String> tokenToUsername) {
        return new StubAccountService(tokenToUsername);
    }

    // -------- StatsService --------

    public static StatsService createStatsService(
            AccountService accountService,
            PlayerGameRepository playerGames,
            GameRepository gameRepo,
            GameClock clock) {
        return new StatsServiceImpl(accountService, playerGames, gameRepo, clock);
    }

    // -------- LeaderboardService --------

    public static LeaderboardService createLeaderboardService(
            AccountService accountService,
            PlayerGameRepository playerGames,
            GameRepository gameRepo) {
        return new LeaderboardServiceImpl(accountService, playerGames, gameRepo);
    }

    // ============================================================
    // Private stub and in-memory implementations
    // ============================================================

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
        private final Map<PlayerGameKey, PlayerGame> store = new ConcurrentHashMap<>();

        @Override
        public PlayerGame findOrCreate(String username, long gameId) {
            PlayerGameKey key = new PlayerGameKey(username, gameId);
            return store.computeIfAbsent(key, k -> new PlayerGame(username, gameId, List.of()));
        }

        @Override
        public void save(PlayerGame playerGame) {
            store.put(new PlayerGameKey(playerGame.username(), playerGame.gameId()), playerGame);
        }

        @Override
        public List<PlayerGame> findByGame(long gameId) {
            return store.values().stream()
                    .filter(pg -> pg.gameId() == gameId)
                    .toList();
        }

        @Override
        public List<PlayerGame> findPlayerGameByUsername(String username) {
            return new ArrayList<>(store.values().stream()
                    .filter(pg -> pg.username().equals(username))
                    .toList());
        }

        @Override
        public Set<String> findAllUsernames() {
            Set<String> usernames = new HashSet<>();
            store.values().forEach(pg -> usernames.add(pg.username()));
            return usernames;
        }

        @Override
        public Optional<PlayerGame> findByUsernameAndGame(String username, long gameId) {
            return Optional.ofNullable(store.get(new PlayerGameKey(username, gameId)));
        }

        @Override
        public List<PlayerGame> findAll() {
            return List.copyOf(store.values());
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

        // Unused methods
        @Override public RegisterData register(String username, String password) { throw new UnsupportedOperationException(); }
        @Override public LoginData login(String username, String password, int udpPort, String remoteAddress) { throw new UnsupportedOperationException(); }
        @Override public void logout(String accountToken) { throw new UnsupportedOperationException(); }
        @Override public UpdateCredentialsData updateCredentials(String oldUsername, String newUsername, String oldPassword, String newPassword) { throw new UnsupportedOperationException(); }
    }
}