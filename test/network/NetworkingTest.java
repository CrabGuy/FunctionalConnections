package test.network;

import server.account.AccountService;
import server.account.exceptions.*;
import server.game.ProposalService;
import server.game.exceptions.*;
import server.network.RequestDispatcher;
import server.stats.LeaderboardService;
import server.stats.StatsService;
import shared.dto.*;

import java.util.List;
import java.util.Map;

//TODO: inconsistent deafault stub creation

/**
 * Simple test runner for the Networking slice (Slice E).
 * Tests the RequestDispatcher component using stub services.
 * Stub implementations reside in {@link NetworkingTestFactory}.
 */
public class NetworkingTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("Running Networking slice tests...\n");

        runTest("testRegisterSuccess", NetworkingTest::testRegisterSuccess);
        runTest("testRegisterUsernameAlreadyRegistered", NetworkingTest::testRegisterUsernameAlreadyRegistered);
        runTest("testLoginSuccess", NetworkingTest::testLoginSuccess);
        runTest("testLoginIncorrectPassword", NetworkingTest::testLoginIncorrectPassword);
        runTest("testLogoutSuccess", NetworkingTest::testLogoutSuccess);
        runTest("testLogoutInvalidToken", NetworkingTest::testLogoutInvalidToken);
        runTest("testUpdateCredentialsSuccess", NetworkingTest::testUpdateCredentialsSuccess);
        runTest("testUpdateCredentialsIncorrectPassword", NetworkingTest::testUpdateCredentialsIncorrectPassword);
        runTest("testUpdateCredentialsNewUsernameTaken", NetworkingTest::testUpdateCredentialsNewUsernameTaken);
        runTest("testSubmitProposalSuccess", NetworkingTest::testSubmitProposalSuccess);
        runTest("testSubmitProposalInvalidProposal", NetworkingTest::testSubmitProposalInvalidProposal);
        runTest("testSubmitProposalGameNotCurrent", NetworkingTest::testSubmitProposalGameNotCurrent);
        runTest("testSubmitProposalPlayerAlreadyCompleted", NetworkingTest::testSubmitProposalPlayerAlreadyCompleted);
        runTest("testSubmitProposalInvalidToken", NetworkingTest::testSubmitProposalInvalidToken);
        runTest("testRequestGameInfoSuccess", NetworkingTest::testRequestGameInfoSuccess);
        runTest("testRequestGameInfoGameNotFound", NetworkingTest::testRequestGameInfoGameNotFound);
        runTest("testRequestGameInfoInvalidToken", NetworkingTest::testRequestGameInfoInvalidToken);
        runTest("testRequestGameStatsSuccess", NetworkingTest::testRequestGameStatsSuccess);
        runTest("testRequestGameStatsGameNotFound", NetworkingTest::testRequestGameStatsGameNotFound);
        runTest("testRequestGameStatsInvalidToken", NetworkingTest::testRequestGameStatsInvalidToken);
        runTest("testRequestLeaderboardSuccess", NetworkingTest::testRequestLeaderboardSuccess);
        runTest("testRequestLeaderboardInvalidToken", NetworkingTest::testRequestLeaderboardInvalidToken);
        runTest("testRequestPlayerStatsSuccess", NetworkingTest::testRequestPlayerStatsSuccess);
        runTest("testRequestPlayerStatsInvalidToken", NetworkingTest::testRequestPlayerStatsInvalidToken);
        runTest("testUnknownOperation", NetworkingTest::testUnknownOperation);
        runTest("testInternalErrorMapping", NetworkingTest::testInternalErrorMapping);

        System.out.println("\n-----------------------------------");
        System.out.println("Tests passed: " + passed);
        System.out.println("Tests failed: " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    @FunctionalInterface
    private interface TestMethod {
        void run() throws Exception;
    }

    private static void runTest(String name, TestMethod test) {
        try {
            test.run();
            System.out.println("[PASS] " + name);
            passed++;
        } catch (Throwable t) {
            System.out.println("[FAIL] " + name + " - " + t.getMessage());
            failed++;
        }
    }

    // ---------------------- Helper assertion methods ----------------------

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // ---------------------- Helper factory method for dispatcher with all stubs ----------------------

    private static RequestDispatcher createDispatcherWithDefaultStubs() {
        return NetworkingTestFactory.createRequestDispatcher(
                new NetworkingTestFactory.StubAccountService(),
                new NetworkingTestFactory.StubProposalService(),
                new NetworkingTestFactory.StubStatsService(),
                new NetworkingTestFactory.StubLeaderboardService()
        );
    }

    // ---------------------- Test methods ----------------------

    private static void testRegisterSuccess() {
        AccountService accountService = new NetworkingTestFactory.StubAccountService() {
            @Override public RegisterData register(String username, String password) {
                return new RegisterData(username);
            }
        };
        RequestDispatcher dispatcher = NetworkingTestFactory.createRequestDispatcher(
                accountService, new NetworkingTestFactory.StubProposalService(),
                new NetworkingTestFactory.StubStatsService(),
                new NetworkingTestFactory.StubLeaderboardService());

        ApiRequest request = new RegisterRequest("alice", "secret");
        ApiResponse<?> response = dispatcher.dispatch(request);

        check(response.success(), "register should succeed");
        check(response.error() == null, "register should have no error");
        check(response.data() instanceof RegisterData, "data should be RegisterData");
        RegisterData data = (RegisterData) response.data();
        check("alice".equals(data.username()), "username should be alice");
    }

    private static void testRegisterUsernameAlreadyRegistered() {
        AccountService accountService = new NetworkingTestFactory.StubAccountService() {
            @Override public RegisterData register(String username, String password) {
                throw new UsernameAlreadyRegisteredException(username);
            }
        };
        RequestDispatcher dispatcher = NetworkingTestFactory.createRequestDispatcher(
                accountService, new NetworkingTestFactory.StubProposalService(),
                new NetworkingTestFactory.StubStatsService(),
                new NetworkingTestFactory.StubLeaderboardService());

        ApiRequest request = new RegisterRequest("alice", "secret");
        ApiResponse<?> response = dispatcher.dispatch(request);

        check(!response.success(), "register should fail");
        check(response.error() != null, "error should be present");
        check(response.error().code() == ErrorCode.USERNAME_ALREADY_REGISTERED, "error code should be USERNAME_ALREADY_REGISTERED");
        check(response.data() == null, "data should be null on error");
    }

    private static void testLoginSuccess() {
        AccountService accountService = new NetworkingTestFactory.StubAccountService() {
            @Override public LoginData login(String username, String password, int udpPort, String remoteAddress) {
                return new LoginData("token-abc");
            }
        };
        RequestDispatcher dispatcher = NetworkingTestFactory.createRequestDispatcher(
                accountService, new NetworkingTestFactory.StubProposalService(),
                new NetworkingTestFactory.StubStatsService(),
                new NetworkingTestFactory.StubLeaderboardService());

        ApiRequest request = new LoginRequest("alice", "secret", 5001);
        ApiResponse<?> response = dispatcher.dispatch(request);

        check(response.success(), "login should succeed");
        check(response.data() instanceof LoginData, "data should be LoginData");
        LoginData data = (LoginData) response.data();
        check("token-abc".equals(data.accountToken()), "account token should be token-abc");
    }

    private static void testLoginIncorrectPassword() {
        AccountService accountService = new NetworkingTestFactory.StubAccountService() {
            @Override public LoginData login(String username, String password, int udpPort, String remoteAddress) {
                throw new IncorrectPasswordException(username);
            }
        };
        RequestDispatcher dispatcher = NetworkingTestFactory.createRequestDispatcher(
                accountService, new NetworkingTestFactory.StubProposalService(),
                new NetworkingTestFactory.StubStatsService(),
                new NetworkingTestFactory.StubLeaderboardService());

        ApiRequest request = new LoginRequest("alice", "wrong", 5001);
        ApiResponse<?> response = dispatcher.dispatch(request);

        check(!response.success(), "login should fail");
        check(response.error() != null, "error should be present");
        check(response.error().code() == ErrorCode.INCORRECT_PASSWORD, "error code should be INCORRECT_PASSWORD");
    }

    private static void testLogoutSuccess() {
        AccountService accountService = new NetworkingTestFactory.StubAccountService() {
            @Override public void logout(String accountToken) {
                // do nothing – success
            }
        };
        RequestDispatcher dispatcher = NetworkingTestFactory.createRequestDispatcher(
                accountService, new NetworkingTestFactory.StubProposalService(),
                new NetworkingTestFactory.StubStatsService(),
                new NetworkingTestFactory.StubLeaderboardService());

        ApiRequest request = new LogoutRequest("token-abc");
        ApiResponse<?> response = dispatcher.dispatch(request);

        check(response.success(), "logout should succeed");
        check(response.data() instanceof LogoutData, "data should be LogoutData");
    }

    private static void testLogoutInvalidToken() {
        AccountService accountService = new NetworkingTestFactory.StubAccountService() {
            @Override public void logout(String accountToken) {
                throw new InvalidTokenException("bad token");
            }
        };
        RequestDispatcher dispatcher = NetworkingTestFactory.createRequestDispatcher(
                accountService, new NetworkingTestFactory.StubProposalService(),
                new NetworkingTestFactory.StubStatsService(),
                new NetworkingTestFactory.StubLeaderboardService());

        ApiRequest request = new LogoutRequest("bad-token");
        ApiResponse<?> response = dispatcher.dispatch(request);

        check(!response.success(), "logout should fail");
        check(response.error() != null, "error should be present");
        check(response.error().code() == ErrorCode.USER_NOT_LOGGED_IN, "error code should be USER_NOT_LOGGED_IN");
    }

    private static void testUpdateCredentialsSuccess() {
        AccountService accountService = new NetworkingTestFactory.StubAccountService() {
            @Override public UpdateCredentialsData updateCredentials(String oldUsername, String newUsername, String oldPassword, String newPassword) {
                return new UpdateCredentialsData(newUsername);
            }
        };
        RequestDispatcher dispatcher = NetworkingTestFactory.createRequestDispatcher(
                accountService, new NetworkingTestFactory.StubProposalService(),
                new NetworkingTestFactory.StubStatsService(),
                new NetworkingTestFactory.StubLeaderboardService());

        ApiRequest request = new UpdateCredentialsRequest("alice", "alice2", "oldpass", "newpass");
        ApiResponse<?> response = dispatcher.dispatch(request);

        check(response.success(), "updateCredentials should succeed");
        check(response.data() instanceof UpdateCredentialsData, "data should be UpdateCredentialsData");
        UpdateCredentialsData data = (UpdateCredentialsData) response.data();
        check("alice2".equals(data.newUsername()), "new username should be alice2");
    }

    private static void testUpdateCredentialsIncorrectPassword() {
        AccountService accountService = new NetworkingTestFactory.StubAccountService() {
            @Override public UpdateCredentialsData updateCredentials(String oldUsername, String newUsername, String oldPassword, String newPassword) {
                throw new IncorrectPasswordException(oldUsername);
            }
        };
        RequestDispatcher dispatcher = NetworkingTestFactory.createRequestDispatcher(
                accountService, new NetworkingTestFactory.StubProposalService(),
                new NetworkingTestFactory.StubStatsService(),
                new NetworkingTestFactory.StubLeaderboardService());

        ApiRequest request = new UpdateCredentialsRequest("alice", "alice2", "wrongold", "newpass");
        ApiResponse<?> response = dispatcher.dispatch(request);

        check(!response.success(), "updateCredentials should fail");
        check(response.error().code() == ErrorCode.INCORRECT_PASSWORD, "error code should be INCORRECT_PASSWORD");
    }

    private static void testUpdateCredentialsNewUsernameTaken() {
        AccountService accountService = new NetworkingTestFactory.StubAccountService() {
            @Override public UpdateCredentialsData updateCredentials(String oldUsername, String newUsername, String oldPassword, String newPassword) {
                throw new NewUsernameAlreadyTakenException(newUsername);
            }
        };
        RequestDispatcher dispatcher = NetworkingTestFactory.createRequestDispatcher(
                accountService, new NetworkingTestFactory.StubProposalService(),
                new NetworkingTestFactory.StubStatsService(),
                new NetworkingTestFactory.StubLeaderboardService());

        ApiRequest request = new UpdateCredentialsRequest("alice", "bob", "oldpass", "newpass");
        ApiResponse<?> response = dispatcher.dispatch(request);

        check(!response.success(), "updateCredentials should fail");
        check(response.error().code() == ErrorCode.NEW_USERNAME_ALREADY_TAKEN, "error code should be NEW_USERNAME_ALREADY_TAKEN");
    }

    private static void testSubmitProposalSuccess() {
        ProposalService proposalService = new NetworkingTestFactory.StubProposalService() {
            @Override public GameInfoData submitProposal(String accountToken, long gameId, List<String> words) {
                return new GameInfoData(gameId, 123456L,
                        List.of("red", "blue", "green", "yellow"),
                        List.of(), List.of(), null);
            }
        };
        RequestDispatcher dispatcher = NetworkingTestFactory.createRequestDispatcher(
                new NetworkingTestFactory.StubAccountService(), proposalService,
                new NetworkingTestFactory.StubStatsService(),
                new NetworkingTestFactory.StubLeaderboardService());

        ApiRequest request = new SubmitProposalRequest("token", List.of("red", "blue", "green", "yellow"));
        ApiResponse<?> response = dispatcher.dispatch(request);

        check(response.success(), "submitProposal should succeed");
        check(response.data() instanceof GameInfoData, "data should be GameInfoData");
    }

    private static void testSubmitProposalInvalidProposal() {
        ProposalService proposalService = new NetworkingTestFactory.StubProposalService() {
            @Override public GameInfoData submitProposal(String accountToken, long gameId, List<String> words) {
                throw new MalformedProposalException();
            }
        };
        RequestDispatcher dispatcher = NetworkingTestFactory.createRequestDispatcher(
                new NetworkingTestFactory.StubAccountService(), proposalService,
                new NetworkingTestFactory.StubStatsService(),
                new NetworkingTestFactory.StubLeaderboardService());

        ApiRequest request = new SubmitProposalRequest("token", List.of("red", "blue"));
        ApiResponse<?> response = dispatcher.dispatch(request);

        check(!response.success(), "submitProposal should fail");
        check(response.error().code() == ErrorCode.MALFORMED_PROPOSAL, "error code should be MALFORMED_PROPOSAL");
    }

    private static void testSubmitProposalGameNotCurrent() {
        ProposalService proposalService = new NetworkingTestFactory.StubProposalService() {
            @Override public GameInfoData submitProposal(String accountToken, long gameId, List<String> words) {
                throw new GameNotCurrentException(gameId, 42L);
            }
        };
        RequestDispatcher dispatcher = NetworkingTestFactory.createRequestDispatcher(
                new NetworkingTestFactory.StubAccountService(), proposalService,
                new NetworkingTestFactory.StubStatsService(),
                new NetworkingTestFactory.StubLeaderboardService());

        ApiRequest request = new SubmitProposalRequest("token", List.of("red", "blue", "green", "yellow"));
        ApiResponse<?> response = dispatcher.dispatch(request);

        check(!response.success(), "submitProposal should fail");
        check(response.error().code() == ErrorCode.GAME_NOT_CURRENT, "error code should be GAME_NOT_CURRENT");
    }

    private static void testSubmitProposalPlayerAlreadyCompleted() {
        ProposalService proposalService = new NetworkingTestFactory.StubProposalService() {
            @Override public GameInfoData submitProposal(String accountToken, long gameId, List<String> words) {
                throw new PlayerAlreadyCompletedGameException("alice", gameId);
            }
        };
        RequestDispatcher dispatcher = NetworkingTestFactory.createRequestDispatcher(
                new NetworkingTestFactory.StubAccountService(), proposalService,
                new NetworkingTestFactory.StubStatsService(),
                new NetworkingTestFactory.StubLeaderboardService());

        ApiRequest request = new SubmitProposalRequest("token", List.of("red", "blue", "green", "yellow"));
        ApiResponse<?> response = dispatcher.dispatch(request);

        check(!response.success(), "submitProposal should fail");
        check(response.error().code() == ErrorCode.PLAYER_ALREADY_COMPLETED_GAME, "error code should be PLAYER_ALREADY_COMPLETED_GAME");
    }

    private static void testSubmitProposalInvalidToken() {
        ProposalService proposalService = new NetworkingTestFactory.StubProposalService() {
            @Override public GameInfoData submitProposal(String accountToken, long gameId, List<String> words) {
                throw new InvalidTokenException("bad token");
            }
        };
        RequestDispatcher dispatcher = NetworkingTestFactory.createRequestDispatcher(
                new NetworkingTestFactory.StubAccountService(), proposalService,
                new NetworkingTestFactory.StubStatsService(),
                new NetworkingTestFactory.StubLeaderboardService());

        ApiRequest request = new SubmitProposalRequest("bad-token", List.of("red", "blue", "green", "yellow"));
        ApiResponse<?> response = dispatcher.dispatch(request);

        check(!response.success(), "submitProposal should fail");
        check(response.error().code() == ErrorCode.USER_NOT_LOGGED_IN, "error code should be USER_NOT_LOGGED_IN");
    }

    private static void testRequestGameInfoSuccess() {
        ProposalService proposalService = new NetworkingTestFactory.StubProposalService() {
            @Override public GameInfoData getGameInfo(String accountToken, Long gameId) {
                return new GameInfoData(1L, 123456L,
                        List.of("cat", "dog", "bird", "fish"),
                        List.of(), List.of(), null);
            }
        };
        RequestDispatcher dispatcher = NetworkingTestFactory.createRequestDispatcher(
                new NetworkingTestFactory.StubAccountService(), proposalService,
                new NetworkingTestFactory.StubStatsService(),
                new NetworkingTestFactory.StubLeaderboardService());

        ApiRequest request = new RequestGameInfoRequest("token", 1L);
        ApiResponse<?> response = dispatcher.dispatch(request);

        check(response.success(), "requestGameInfo should succeed");
        check(response.data() instanceof GameInfoData, "data should be GameInfoData");
    }

    private static void testRequestGameInfoGameNotFound() {
        ProposalService proposalService = new NetworkingTestFactory.StubProposalService() {
            @Override public GameInfoData getGameInfo(String accountToken, Long gameId) {
                throw new GameNotFoundException(gameId);
            }
        };
        RequestDispatcher dispatcher = NetworkingTestFactory.createRequestDispatcher(
                new NetworkingTestFactory.StubAccountService(), proposalService,
                new NetworkingTestFactory.StubStatsService(),
                new NetworkingTestFactory.StubLeaderboardService());

        ApiRequest request = new RequestGameInfoRequest("token", 999L);
        ApiResponse<?> response = dispatcher.dispatch(request);

        check(!response.success(), "requestGameInfo should fail");
        check(response.error().code() == ErrorCode.GAME_NOT_FOUND, "error code should be GAME_NOT_FOUND");
    }

    private static void testRequestGameInfoInvalidToken() {
        ProposalService proposalService = new NetworkingTestFactory.StubProposalService() {
            @Override public GameInfoData getGameInfo(String accountToken, Long gameId) {
                throw new InvalidTokenException("bad token");
            }
        };
        RequestDispatcher dispatcher = NetworkingTestFactory.createRequestDispatcher(
                new NetworkingTestFactory.StubAccountService(), proposalService,
                new NetworkingTestFactory.StubStatsService(),
                new NetworkingTestFactory.StubLeaderboardService());

        ApiRequest request = new RequestGameInfoRequest("bad-token", 1L);
        ApiResponse<?> response = dispatcher.dispatch(request);

        check(!response.success(), "requestGameInfo should fail");
        check(response.error().code() == ErrorCode.USER_NOT_LOGGED_IN, "error code should be USER_NOT_LOGGED_IN");
    }

    private static void testRequestGameStatsSuccess() {
        StatsService statsService = new NetworkingTestFactory.StubStatsService() {
            @Override public GameStatsData getGameStats(String accountToken, Long gameId) {
                return new GameStatsData(gameId, true, 123456L, 10, 5, 3, 2, 7.5);
            }
        };
        RequestDispatcher dispatcher = NetworkingTestFactory.createRequestDispatcher(
                new NetworkingTestFactory.StubAccountService(),
                new NetworkingTestFactory.StubProposalService(),
                statsService,
                new NetworkingTestFactory.StubLeaderboardService());

        ApiRequest request = new RequestGameStatsRequest("token", 1L);
        ApiResponse<?> response = dispatcher.dispatch(request);

        check(response.success(), "requestGameStats should succeed");
        check(response.data() instanceof GameStatsData, "data should be GameStatsData");
    }

    private static void testRequestGameStatsGameNotFound() {
        StatsService statsService = new NetworkingTestFactory.StubStatsService() {
            @Override public GameStatsData getGameStats(String accountToken, Long gameId) {
                throw new GameNotFoundException(gameId);
            }
        };
        RequestDispatcher dispatcher = NetworkingTestFactory.createRequestDispatcher(
                new NetworkingTestFactory.StubAccountService(),
                new NetworkingTestFactory.StubProposalService(),
                statsService,
                new NetworkingTestFactory.StubLeaderboardService());

        ApiRequest request = new RequestGameStatsRequest("token", 999L);
        ApiResponse<?> response = dispatcher.dispatch(request);

        check(!response.success(), "requestGameStats should fail");
        check(response.error().code() == ErrorCode.GAME_NOT_FOUND, "error code should be GAME_NOT_FOUND");
    }

    private static void testRequestGameStatsInvalidToken() {
        StatsService statsService = new NetworkingTestFactory.StubStatsService() {
            @Override public GameStatsData getGameStats(String accountToken, Long gameId) {
                throw new InvalidTokenException("bad token");
            }
        };
        RequestDispatcher dispatcher = NetworkingTestFactory.createRequestDispatcher(
                new NetworkingTestFactory.StubAccountService(),
                new NetworkingTestFactory.StubProposalService(),
                statsService,
                new NetworkingTestFactory.StubLeaderboardService());

        ApiRequest request = new RequestGameStatsRequest("bad-token", 1L);
        ApiResponse<?> response = dispatcher.dispatch(request);

        check(!response.success(), "requestGameStats should fail");
        check(response.error().code() == ErrorCode.USER_NOT_LOGGED_IN, "error code should be USER_NOT_LOGGED_IN");
    }

    private static void testRequestLeaderboardSuccess() {
        LeaderboardService leaderboardService = new NetworkingTestFactory.StubLeaderboardService() {
            @Override public LeaderboardData getLeaderboard(String accountToken, String playerName, Integer topK) {
                return new LeaderboardData(
                        List.of(new LeaderboardEntry("alice", 100, 1)),
                        new LeaderboardEntry("bob", 80, 2),
                        10
                );
            }
        };
        RequestDispatcher dispatcher = NetworkingTestFactory.createRequestDispatcher(
                new NetworkingTestFactory.StubAccountService(),
                new NetworkingTestFactory.StubProposalService(),
                new NetworkingTestFactory.StubStatsService(),
                leaderboardService);

        ApiRequest request = new RequestLeaderboardRequest("token", "bob", 3);
        ApiResponse<?> response = dispatcher.dispatch(request);

        check(response.success(), "requestLeaderboard should succeed");
        check(response.data() instanceof LeaderboardData, "data should be LeaderboardData");
    }

    private static void testRequestLeaderboardInvalidToken() {
        LeaderboardService leaderboardService = new NetworkingTestFactory.StubLeaderboardService() {
            @Override public LeaderboardData getLeaderboard(String accountToken, String playerName, Integer topK) {
                throw new InvalidTokenException("bad token");
            }
        };
        RequestDispatcher dispatcher = NetworkingTestFactory.createRequestDispatcher(
                new NetworkingTestFactory.StubAccountService(),
                new NetworkingTestFactory.StubProposalService(),
                new NetworkingTestFactory.StubStatsService(),
                leaderboardService);

        ApiRequest request = new RequestLeaderboardRequest("bad-token", null, null);
        ApiResponse<?> response = dispatcher.dispatch(request);

        check(!response.success(), "requestLeaderboard should fail");
        check(response.error().code() == ErrorCode.USER_NOT_LOGGED_IN, "error code should be USER_NOT_LOGGED_IN");
    }

    private static void testRequestPlayerStatsSuccess() {
        StatsService statsService = new NetworkingTestFactory.StubStatsService() {
            @Override public PlayerStatsData getPlayerStats(String accountToken) {
                return new PlayerStatsData(10, 0.6, 0.4, 2, 5, 3, Map.of(0, 1, 2, 3));
            }
        };
        RequestDispatcher dispatcher = NetworkingTestFactory.createRequestDispatcher(
                new NetworkingTestFactory.StubAccountService(),
                new NetworkingTestFactory.StubProposalService(),
                statsService,
                new NetworkingTestFactory.StubLeaderboardService());

        ApiRequest request = new RequestPlayerStatsRequest("token");
        ApiResponse<?> response = dispatcher.dispatch(request);

        check(response.success(), "requestPlayerStats should succeed");
        check(response.data() instanceof PlayerStatsData, "data should be PlayerStatsData");
    }

    private static void testRequestPlayerStatsInvalidToken() {
        StatsService statsService = new NetworkingTestFactory.StubStatsService() {
            @Override public PlayerStatsData getPlayerStats(String accountToken) {
                throw new InvalidTokenException("bad token");
            }
        };
        RequestDispatcher dispatcher = NetworkingTestFactory.createRequestDispatcher(
                new NetworkingTestFactory.StubAccountService(),
                new NetworkingTestFactory.StubProposalService(),
                statsService,
                new NetworkingTestFactory.StubLeaderboardService());

        ApiRequest request = new RequestPlayerStatsRequest("bad-token");
        ApiResponse<?> response = dispatcher.dispatch(request);

        check(!response.success(), "requestPlayerStats should fail");
        check(response.error().code() == ErrorCode.USER_NOT_LOGGED_IN, "error code should be USER_NOT_LOGGED_IN");
    }

    private static void testUnknownOperation() {
        RequestDispatcher dispatcher = createDispatcherWithDefaultStubs();

        ApiRequest request = new ApiRequest() {
            @Override public String getOperation() { return "unknown"; }
        };

        ApiResponse<?> response = dispatcher.dispatch(request);
        check(!response.success(), "unknown operation should fail");
        check(response.error() != null, "error should be present");
        check(response.error().code() == ErrorCode.INTERNAL_ERROR, "error code should be INTERNAL_ERROR");
    }

    private static void testInternalErrorMapping() {
        AccountService accountService = new NetworkingTestFactory.StubAccountService() {
            @Override public RegisterData register(String username, String password) {
                throw new RuntimeException("Unexpected failure");
            }
        };
        RequestDispatcher dispatcher = NetworkingTestFactory.createRequestDispatcher(
                accountService, new NetworkingTestFactory.StubProposalService(),
                new NetworkingTestFactory.StubStatsService(),
                new NetworkingTestFactory.StubLeaderboardService());

        ApiRequest request = new RegisterRequest("alice", "secret");
        ApiResponse<?> response = dispatcher.dispatch(request);

        check(!response.success(), "register should fail");
        check(response.error() != null, "error should be present");
        check(response.error().code() == ErrorCode.INTERNAL_ERROR, "error code should be INTERNAL_ERROR");
    }
}