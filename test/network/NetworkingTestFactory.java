package test.network;

import server.account.AccountService;
import server.dto.AccountPrincipal;
import server.game.ProposalService;
import server.game.GameClock;
import server.network.RequestDispatcher;
import server.network.RequestDispatcherImpl;
import server.stats.LeaderboardService;
import server.stats.StatsService;
import shared.dto.*;

import java.util.List;

/**
 * Factory for creating test instances of the Networking slice components (Slice E).
 * Provides stub implementations for unit testing RequestDispatcher.
 */
public class NetworkingTestFactory {

    // ------------------------------------------------------------------
    // Stub implementations (nested for convenience)
    // ------------------------------------------------------------------

    /** Stub AccountService – all methods throw UnsupportedOperationException unless overridden. */
    public static class StubAccountService implements AccountService {
        @Override public RegisterData register(String username, String password) { throw new UnsupportedOperationException(); }
        @Override public LoginData login(String username, String password, int udpPort, String remoteAddress) { throw new UnsupportedOperationException(); }
        @Override public void logout(String accountToken) { throw new UnsupportedOperationException(); }
        @Override public UpdateCredentialsData updateCredentials(String oldUsername, String newUsername, String oldPassword, String newPassword) { throw new UnsupportedOperationException(); }
        @Override public AccountPrincipal resolve(String accountToken) { throw new UnsupportedOperationException(); }
    }

    /** Stub ProposalService – all methods throw UnsupportedOperationException unless overridden. */
    public static class StubProposalService implements ProposalService {
        @Override public GameInfoData submitProposal(String accountToken, long gameId, List<String> words) { throw new UnsupportedOperationException(); }
        @Override public GameInfoData getGameInfo(String accountToken, Long gameId) { throw new UnsupportedOperationException(); }
        @Override public GameInfoData getGameInfoForUsername(long gameId, String username) { throw new UnsupportedOperationException(); }
    }

    /** Stub StatsService – all methods throw UnsupportedOperationException unless overridden. */
    public static class StubStatsService implements StatsService {
        @Override public GameStatsData getGameStats(String accountToken, Long gameId) { throw new UnsupportedOperationException(); }
        @Override public PlayerStatsData getPlayerStats(String accountToken) { throw new UnsupportedOperationException(); }
    }

    /** Stub LeaderboardService – all methods throw UnsupportedOperationException unless overridden. */
    public static class StubLeaderboardService implements LeaderboardService {
        @Override public LeaderboardData getLeaderboard(String accountToken, String playerName, Integer topK) { throw new UnsupportedOperationException(); }
    }

    /** Stub GameClock – minimal implementation returning a fixed game id. */
    public static class StubGameClock implements GameClock {
        @Override public long currentGameId(long nowMillis) { return 1L; }
        @Override public long startedAt(long gameId) { return 0L; }
        @Override public long expiresAt(long gameId) { return Long.MAX_VALUE; }
        @Override public boolean isCompleted(long gameId, long nowMillis) { return false; }
    }

    // ------------------------------------------------------------------
    // Factory methods
    // ------------------------------------------------------------------

    /**
     * Creates a RequestDispatcher wired with the given service implementations.
     * Uses a dummy InetSocketAddress and a stub GameClock.
     */
    public static RequestDispatcher createRequestDispatcher(
            AccountService accountService,
            ProposalService proposalService,
            StatsService statsService,
            LeaderboardService leaderboardService) {
        return new RequestDispatcherImpl(
                accountService,
                proposalService,
                statsService,
                leaderboardService,
                new StubGameClock()
        );
    }
}