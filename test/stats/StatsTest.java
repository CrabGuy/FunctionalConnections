package test.stats;

import server.account.AccountService;
import server.game.GameClock;
import server.game.GameRepository;
import server.game.PlayerGameRepository;
import server.stats.LeaderboardService;
import server.stats.StatsService;
import server.dto.*;
import server.game.exceptions.GameNotFoundException;
import shared.dto.*;
import server.account.exceptions.InvalidTokenException;

import java.util.*;

public class StatsTest {

    private static int passed = 0;
    private static int failed = 0;

    // Safe large duration that avoids overflow for game ids up to e.g. 1000.
    private static final long ACTIVE_DURATION = Long.MAX_VALUE / 10;

    public static void main(String[] args) {
        System.out.println("Running Stats & Leaderboard slice tests...\n");

        // Original tests
        runTest("testGameStatsCompleted", StatsTest::testGameStatsCompleted);
        runTest("testGameStatsOngoing", StatsTest::testGameStatsOngoing);
        runTest("testPlayerStats", StatsTest::testPlayerStats);
        runTest("testLeaderboard", StatsTest::testLeaderboard);
        runTest("testInvalidToken", StatsTest::testInvalidToken);
        runTest("testGameNotFound", StatsTest::testGameNotFound);

        // New edge-case tests
        runTest("testPlayerStatsEmptyHistory", StatsTest::testPlayerStatsEmptyHistory);
        runTest("testPlayerStatsAllIncomplete", StatsTest::testPlayerStatsAllIncomplete);
        runTest("testPlayerStatsCurrentStreakOngoingWin", StatsTest::testPlayerStatsCurrentStreakOngoingWin);
        runTest("testPlayerStatsMaxStreakInMiddle", StatsTest::testPlayerStatsMaxStreakInMiddle);
        runTest("testPlayerStatsHistogramExcludesIncomplete", StatsTest::testPlayerStatsHistogramExcludesIncomplete);
        runTest("testGameStatsZeroParticipants", StatsTest::testGameStatsZeroParticipants);
        runTest("testGameStatsAllWinners", StatsTest::testGameStatsAllWinners);
        runTest("testGameStatsAllLosers", StatsTest::testGameStatsAllLosers);
        runTest("testGameStatsActiveVsCompletedOngoing", StatsTest::testGameStatsActiveVsCompletedOngoing);
        runTest("testGameStatsNullGameId", StatsTest::testGameStatsNullGameId);
        runTest("testLeaderboardEmptySystem", StatsTest::testLeaderboardEmptySystem);
        runTest("testLeaderboardTopKBounds", StatsTest::testLeaderboardTopKBounds);
        runTest("testLeaderboardTiedScores", StatsTest::testLeaderboardTiedScores);
        runTest("testLeaderboardNonExistentPlayer", StatsTest::testLeaderboardNonExistentPlayer);

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

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertThrows(Class<? extends Throwable> expectedType, Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message + " (no exception thrown)");
        } catch (Throwable t) {
            if (!expectedType.isInstance(t)) {
                throw new AssertionError(message + " (expected " + expectedType.getSimpleName()
                        + " but got " + t.getClass().getSimpleName() + ")");
            }
        }
    }

    private static GameWordGroups createGame(long gameId) {
        List<WordGroup> groups = List.of(
                new WordGroup("A", List.of("a1","a2","a3","a4")),
                new WordGroup("B", List.of("b1","b2","b3","b4")),
                new WordGroup("C", List.of("c1","c2","c3","c4")),
                new WordGroup("D", List.of("d1","d2","d3","d4"))
        );
        return new GameWordGroups(gameId, groups);
    }

    private static Proposal proposal(String... words) {
        return new Proposal(Set.of(words));
    }

    private static final Proposal CORRECT_A = proposal("a1","a2","a3","a4");
    private static final Proposal CORRECT_B = proposal("b1","b2","b3","b4");
    private static final Proposal CORRECT_C = proposal("c1","c2","c3","c4");
    @SuppressWarnings("unused")
    private static final Proposal CORRECT_D = proposal("d1","d2","d3","d4");
    private static final Proposal WRONG_1 = proposal("a1","b1","c1","d1");

    // ---------------------- Original tests ----------------------

    private static void testGameStatsCompleted() {
        long gameId = 1L;
        GameClock clock = StatsTestFactory.createGameClock(1L); // 1 ms duration → game is completed
        GameWordGroups gameGroups = createGame(gameId);
        GameRepository gameRepo = StatsTestFactory.createGameRepository(Map.of(gameId, gameGroups));
        PlayerGameRepository playerRepo = StatsTestFactory.createPlayerGameRepository();
        AccountService accountService = StatsTestFactory.createAccountService(
                Map.of("token-alice","alice", "token-bob","bob", "token-carol","carol", "token-dave","dave", "token-eve","eve"));

        playerRepo.save(new PlayerGame("alice", gameId, List.of(CORRECT_A, CORRECT_B, CORRECT_C))); // won
        playerRepo.save(new PlayerGame("bob", gameId, List.of(CORRECT_A, WRONG_1, CORRECT_B, CORRECT_C))); // won
        playerRepo.save(new PlayerGame("carol", gameId, List.of(CORRECT_A, CORRECT_B, WRONG_1, WRONG_1, WRONG_1, WRONG_1))); // lost (4 mistakes)
        playerRepo.save(new PlayerGame("dave", gameId, List.of(CORRECT_A, WRONG_1, WRONG_1))); // incomplete
        playerRepo.save(new PlayerGame("eve", gameId, List.of())); // incomplete

        StatsService statsService = StatsTestFactory.createStatsService(accountService, playerRepo, gameRepo, clock);
        GameStatsData stats = statsService.getGameStats("token-alice", gameId);

        check(stats.gameId() == gameId, "gameId should match");
        check(stats.completed(), "game should be completed");
        check(stats.expiresAt() == clock.expiresAt(gameId), "expiresAt should be correct");
        check(stats.totalParticipants() == 5, "totalParticipants should be 5");
        check(stats.activePlayers() == 0, "activePlayers should be 0 (game ended)");
        // Only Alice, Bob, Carol have terminal outcomes (won/lost)
        check(stats.completedPlayers() == 3, "completedPlayers should be 3");
        check(stats.winners() == 2, "winners should be 2");

        // Average score: alice=18, bob=14, carol=-4, dave=-2, eve=0 => sum=26, avg=5.2
        double expectedAvg = 5.2;
        check(Math.abs(stats.averageScore() - expectedAvg) < 0.001, "averageScore should be 5.2");
    }

    private static void testGameStatsOngoing() {
        long gameId = 0L;
        GameClock clock = StatsTestFactory.createGameClock(ACTIVE_DURATION);
        GameWordGroups gameGroups = createGame(gameId);
        GameRepository gameRepo = StatsTestFactory.createGameRepository(Map.of(gameId, gameGroups));
        PlayerGameRepository playerRepo = StatsTestFactory.createPlayerGameRepository();
        AccountService accountService = StatsTestFactory.createAccountService(
                Map.of("token-alice","alice", "token-bob","bob", "token-carol","carol", "token-dave","dave", "token-eve","eve"));

        playerRepo.save(new PlayerGame("alice", gameId, List.of(CORRECT_A, CORRECT_B, CORRECT_C)));
        playerRepo.save(new PlayerGame("bob", gameId, List.of(CORRECT_A, WRONG_1, CORRECT_B, CORRECT_C)));
        playerRepo.save(new PlayerGame("carol", gameId, List.of(CORRECT_A, CORRECT_B, WRONG_1, WRONG_1, WRONG_1, WRONG_1)));
        playerRepo.save(new PlayerGame("dave", gameId, List.of(CORRECT_A, WRONG_1, WRONG_1)));
        playerRepo.save(new PlayerGame("eve", gameId, List.of()));

        StatsService statsService = StatsTestFactory.createStatsService(accountService, playerRepo, gameRepo, clock);
        GameStatsData stats = statsService.getGameStats("token-alice", gameId);

        check(!stats.completed(), "game should be ongoing");
        check(stats.totalParticipants() == 5, "totalParticipants should be 5");
        check(stats.activePlayers() == 2, "activePlayers should be 2 (dave, eve)");
        check(stats.completedPlayers() == 3, "completedPlayers should be 3");
        check(stats.winners() == 2, "winners should be 2");
        check(Math.abs(stats.averageScore() - 5.2) < 0.001, "averageScore should be 5.2");
    }

    private static void testPlayerStats() {
        GameClock clock = StatsTestFactory.createGameClock(ACTIVE_DURATION);
        Map<Long, GameWordGroups> games = new HashMap<>();
        for (long gid = 0; gid < 5; gid++) {
            games.put(gid, createGame(gid));
        }
        GameRepository gameRepo = StatsTestFactory.createGameRepository(games);
        PlayerGameRepository playerRepo = StatsTestFactory.createPlayerGameRepository();
        AccountService accountService = StatsTestFactory.createAccountService(Map.of("token-alice","alice"));

        playerRepo.save(new PlayerGame("alice", 0L, List.of(CORRECT_A, CORRECT_B, CORRECT_C))); // win
        playerRepo.save(new PlayerGame("alice", 1L, List.of(CORRECT_A, WRONG_1, CORRECT_B, WRONG_1, CORRECT_C))); // win
        playerRepo.save(new PlayerGame("alice", 2L, List.of(WRONG_1, WRONG_1, WRONG_1, WRONG_1))); // loss
        playerRepo.save(new PlayerGame("alice", 3L, List.of(CORRECT_A, CORRECT_B, WRONG_1))); // incomplete
        playerRepo.save(new PlayerGame("alice", 4L, List.of(CORRECT_A))); // incomplete

        StatsService statsService = StatsTestFactory.createStatsService(accountService, playerRepo, gameRepo, clock);
        PlayerStatsData ps = statsService.getPlayerStats("token-alice");

        check(ps.puzzlesCompleted() == 3, "puzzlesCompleted should be 3");
        double expectedWinRate = (2.0/3.0)*100;
        double expectedLossRate = (1.0/3.0)*100;
        check(Math.abs(ps.winRate() - expectedWinRate) < 0.001, "winRate should be ~66.67");
        check(Math.abs(ps.lossRate() - expectedLossRate) < 0.001, "lossRate should be ~33.33");
        check(ps.currentStreak() == 0, "currentStreak should be 0");
        check(ps.maxStreak() == 2, "maxStreak should be 2");
        check(ps.perfectPuzzles() == 1, "perfectPuzzles should be 1");
        
        Map<Integer, Integer> expectedHist = new HashMap<>();
        expectedHist.put(0, 1);
        expectedHist.put(1, 0);
        expectedHist.put(2, 1);
        expectedHist.put(3, 0);
        check(ps.mistakeHistogram().equals(expectedHist),
            "mistakeHistogram should be {0:1, 1:0, 2:1, 3:0}");
    }

    private static void testLeaderboard() {
        @SuppressWarnings("unused")
        GameClock clock = StatsTestFactory.createGameClock(ACTIVE_DURATION);
        Map<Long, GameWordGroups> games = new HashMap<>();
        for (long gid = 0; gid < 3; gid++) {
            games.put(gid, createGame(gid));
        }
        GameRepository gameRepo = StatsTestFactory.createGameRepository(games);
        PlayerGameRepository playerRepo = StatsTestFactory.createPlayerGameRepository();
        AccountService accountService = StatsTestFactory.createAccountService(
                Map.of("token-alice","alice", "token-bob","bob", "token-carol","carol", "token-dave","dave"));

        playerRepo.save(new PlayerGame("alice", 0L, List.of(CORRECT_A, CORRECT_B, CORRECT_C)));
        playerRepo.save(new PlayerGame("alice", 1L, List.of(CORRECT_A)));

        playerRepo.save(new PlayerGame("bob", 0L, List.of(CORRECT_A, WRONG_1, CORRECT_B, CORRECT_C)));
        playerRepo.save(new PlayerGame("bob", 1L, List.of(WRONG_1, WRONG_1, WRONG_1, WRONG_1)));
        playerRepo.save(new PlayerGame("bob", 2L, List.of(CORRECT_A, CORRECT_B, CORRECT_C)));

        playerRepo.save(new PlayerGame("carol", 0L, List.of(WRONG_1, WRONG_1, WRONG_1, WRONG_1)));
        playerRepo.save(new PlayerGame("carol", 2L, List.of(CORRECT_A, CORRECT_B, CORRECT_C)));

        playerRepo.save(new PlayerGame("dave", 1L, List.of(CORRECT_A, CORRECT_B, CORRECT_C)));

        LeaderboardService leaderboardService = StatsTestFactory.createLeaderboardService(accountService, playerRepo, gameRepo);

        LeaderboardData full = leaderboardService.getLeaderboard("token-alice", null, null);
        List<LeaderboardEntry> top = full.topPlayers();
        check(top.size() == 4, "topPlayers should contain all 4 players");
        check(top.get(0).username().equals("alice") && top.get(0).score() == 24, "First should be alice with 24");
        check(top.get(1).username().equals("dave") && top.get(1).score() == 18, "Second should be dave with 18");
        check(top.get(2).username().equals("bob") && top.get(2).score() == 16, "Third should be bob with 16");
        check(top.get(3).username().equals("carol") && top.get(3).score() == 2, "Fourth should be carol with 2");

        LeaderboardData top2 = leaderboardService.getLeaderboard("token-bob", null, 2);
        check(top2.topPlayers().size() == 2, "top2 should have 2 entries");
        check(top2.topPlayers().get(0).username().equals("alice") && top2.topPlayers().get(0).score() == 24, "First of top2 should be alice");
        check(top2.topPlayers().get(1).username().equals("dave") && top2.topPlayers().get(1).score() == 18, "Second of top2 should be dave");

        LeaderboardData carolData = leaderboardService.getLeaderboard("token-carol", "carol", null);
        check(carolData.requestedPlayer() != null, "requestedPlayer should be present");
        check(carolData.requestedPlayer().username().equals("carol"), "requestedPlayer username should be carol");
        check(carolData.requestedPlayer().score() == 2, "carol score should be 2");
        check(carolData.requestedPlayer().rank() == 4, "carol rank should be 4");
    }

    private static void testInvalidToken() {
        GameClock clock = StatsTestFactory.createGameClock(ACTIVE_DURATION);
        GameRepository gameRepo = StatsTestFactory.createGameRepository(Map.of(0L, createGame(0L)));
        PlayerGameRepository playerRepo = StatsTestFactory.createPlayerGameRepository();
        AccountService accountService = StatsTestFactory.createAccountService(Map.of("valid-token","alice"));
        StatsService statsService = StatsTestFactory.createStatsService(accountService, playerRepo, gameRepo, clock);

        assertThrows(InvalidTokenException.class,
                () -> statsService.getGameStats("invalid-token", 0L),
                "Invalid token should throw InvalidTokenException");
        assertThrows(InvalidTokenException.class,
                () -> statsService.getPlayerStats("invalid-token"),
                "Invalid token should throw InvalidTokenException");
    }

    private static void testGameNotFound() {
        GameClock clock = StatsTestFactory.createGameClock(ACTIVE_DURATION);
        GameRepository gameRepo = StatsTestFactory.createGameRepository(Map.of(0L, createGame(0L)));
        PlayerGameRepository playerRepo = StatsTestFactory.createPlayerGameRepository();
        AccountService accountService = StatsTestFactory.createAccountService(Map.of("token-alice","alice"));
        StatsService statsService = StatsTestFactory.createStatsService(accountService, playerRepo, gameRepo, clock);

        assertThrows(GameNotFoundException.class,
                () -> statsService.getGameStats("token-alice", 999L),
                "Nonexistent game should throw GameNotFoundException");
    }

    // ---------------------- New edge-case tests ----------------------

    private static void testPlayerStatsEmptyHistory() {
        GameClock clock = StatsTestFactory.createGameClock(ACTIVE_DURATION);
        GameRepository gameRepo = StatsTestFactory.createGameRepository(Map.of(0L, createGame(0L)));
        PlayerGameRepository playerRepo = StatsTestFactory.createPlayerGameRepository();
        AccountService accountService = StatsTestFactory.createAccountService(Map.of("token-alice","alice"));
        StatsService statsService = StatsTestFactory.createStatsService(accountService, playerRepo, gameRepo, clock);

        PlayerStatsData ps = statsService.getPlayerStats("token-alice");

        check(ps.puzzlesCompleted() == 0, "puzzlesCompleted should be 0 for empty history");
        check(ps.winRate() == 0.0, "winRate should be 0.0 with no completed games");
        check(ps.lossRate() == 0.0, "lossRate should be 0.0 with no completed games");
        check(ps.currentStreak() == 0, "currentStreak should be 0");
        check(ps.maxStreak() == 0, "maxStreak should be 0");
        check(ps.perfectPuzzles() == 0, "perfectPuzzles should be 0");
        check(ps.mistakeHistogram().isEmpty() || allZeroHistogram(ps.mistakeHistogram()),
                "histogram should be empty or all zeros for empty history");
    }

    private static void testPlayerStatsAllIncomplete() {
        GameClock clock = StatsTestFactory.createGameClock(ACTIVE_DURATION);
        Map<Long, GameWordGroups> games = new HashMap<>();
        for (long gid = 0; gid < 3; gid++) {
            games.put(gid, createGame(gid));
        }
        GameRepository gameRepo = StatsTestFactory.createGameRepository(games);
        PlayerGameRepository playerRepo = StatsTestFactory.createPlayerGameRepository();
        AccountService accountService = StatsTestFactory.createAccountService(Map.of("token-alice","alice"));

        // All incomplete: never reached 3 correct nor 4 wrong
        playerRepo.save(new PlayerGame("alice", 0L, List.of(CORRECT_A, WRONG_1)));
        playerRepo.save(new PlayerGame("alice", 1L, List.of()));
        playerRepo.save(new PlayerGame("alice", 2L, List.of(CORRECT_A, CORRECT_B, WRONG_1)));

        StatsService statsService = StatsTestFactory.createStatsService(accountService, playerRepo, gameRepo, clock);
        PlayerStatsData ps = statsService.getPlayerStats("token-alice");

        check(ps.puzzlesCompleted() == 0, "puzzlesCompleted should be 0 when all games are incomplete");
        check(ps.winRate() == 0.0, "winRate should be 0.0");
        check(ps.lossRate() == 0.0, "lossRate should be 0.0");
        check(ps.currentStreak() == 0, "currentStreak should be 0");
        check(ps.maxStreak() == 0, "maxStreak should be 0");
        check(ps.perfectPuzzles() == 0, "perfectPuzzles should be 0");
    }

    private static void testPlayerStatsCurrentStreakOngoingWin() {
        GameClock clock = StatsTestFactory.createGameClock(ACTIVE_DURATION);
        Map<Long, GameWordGroups> games = new HashMap<>();
        for (long gid = 0; gid < 2; gid++) {
            games.put(gid, createGame(gid));
        }
        GameRepository gameRepo = StatsTestFactory.createGameRepository(games);
        PlayerGameRepository playerRepo = StatsTestFactory.createPlayerGameRepository();
        AccountService accountService = StatsTestFactory.createAccountService(Map.of("token-alice","alice"));

        playerRepo.save(new PlayerGame("alice", 0L, List.of(CORRECT_A, CORRECT_B, CORRECT_C))); // win
        playerRepo.save(new PlayerGame("alice", 1L, List.of(CORRECT_A, CORRECT_B, CORRECT_C))); // win

        StatsService statsService = StatsTestFactory.createStatsService(accountService, playerRepo, gameRepo, clock);
        PlayerStatsData ps = statsService.getPlayerStats("token-alice");

        check(ps.puzzlesCompleted() == 2, "puzzlesCompleted should be 2");
        check(ps.currentStreak() == 2, "currentStreak should be 2 when latest games are wins");
        check(ps.maxStreak() == 2, "maxStreak should be 2");
    }

    private static void testPlayerStatsMaxStreakInMiddle() {
        GameClock clock = StatsTestFactory.createGameClock(ACTIVE_DURATION);
        Map<Long, GameWordGroups> games = new HashMap<>();
        for (long gid = 0; gid < 4; gid++) {
            games.put(gid, createGame(gid));
        }
        GameRepository gameRepo = StatsTestFactory.createGameRepository(games);
        PlayerGameRepository playerRepo = StatsTestFactory.createPlayerGameRepository();
        AccountService accountService = StatsTestFactory.createAccountService(Map.of("token-alice","alice"));

        playerRepo.save(new PlayerGame("alice", 0L, List.of(CORRECT_A, CORRECT_B, CORRECT_C))); // win
        playerRepo.save(new PlayerGame("alice", 1L, List.of(CORRECT_A, CORRECT_B, CORRECT_C))); // win
        playerRepo.save(new PlayerGame("alice", 2L, List.of(WRONG_1, WRONG_1, WRONG_1, WRONG_1))); // loss
        playerRepo.save(new PlayerGame("alice", 3L, List.of(CORRECT_A, CORRECT_B, CORRECT_C))); // win

        StatsService statsService = StatsTestFactory.createStatsService(accountService, playerRepo, gameRepo, clock);
        PlayerStatsData ps = statsService.getPlayerStats("token-alice");

        check(ps.currentStreak() == 1, "currentStreak should be 1 after a single win following a loss");
        check(ps.maxStreak() == 2, "maxStreak should be 2 (the two consecutive wins in the middle)");
    }

    private static void testPlayerStatsHistogramExcludesIncomplete() {
        GameClock clock = StatsTestFactory.createGameClock(ACTIVE_DURATION);
        Map<Long, GameWordGroups> games = new HashMap<>();
        for (long gid = 0; gid < 3; gid++) {
            games.put(gid, createGame(gid));
        }
        GameRepository gameRepo = StatsTestFactory.createGameRepository(games);
        PlayerGameRepository playerRepo = StatsTestFactory.createPlayerGameRepository();
        AccountService accountService = StatsTestFactory.createAccountService(Map.of("token-alice","alice"));

        playerRepo.save(new PlayerGame("alice", 0L, List.of(CORRECT_A, CORRECT_B, CORRECT_C))); // win, 0 wrong
        playerRepo.save(new PlayerGame("alice", 1L, List.of(CORRECT_A, CORRECT_B, WRONG_1))); // incomplete
        playerRepo.save(new PlayerGame("alice", 2L, List.of(CORRECT_A, WRONG_1, CORRECT_B, WRONG_1, CORRECT_C))); // win, 2 wrong

        StatsService statsService = StatsTestFactory.createStatsService(accountService, playerRepo, gameRepo, clock);
        PlayerStatsData ps = statsService.getPlayerStats("token-alice");

        check(ps.puzzlesCompleted() == 2, "Only completed games count toward puzzlesCompleted");

        // Histogram should only reflect the two completed games, excluding the incomplete one.
        Map<Integer, Integer> expectedHist = new HashMap<>();
        expectedHist.put(0, 1); // game 0
        expectedHist.put(1, 0);
        expectedHist.put(2, 1); // game 2
        expectedHist.put(3, 0);
        check(ps.mistakeHistogram().equals(expectedHist),
                "mistakeHistogram should exclude incomplete games");
    }

    private static void testGameStatsZeroParticipants() {
        long gameId = 0L;
        GameClock clock = StatsTestFactory.createGameClock(1L); // completed
        GameWordGroups gameGroups = createGame(gameId);
        GameRepository gameRepo = StatsTestFactory.createGameRepository(Map.of(gameId, gameGroups));
        PlayerGameRepository playerRepo = StatsTestFactory.createPlayerGameRepository();
        AccountService accountService = StatsTestFactory.createAccountService(Map.of("token-alice","alice"));

        StatsService statsService = StatsTestFactory.createStatsService(accountService, playerRepo, gameRepo, clock);
        GameStatsData stats = statsService.getGameStats("token-alice", gameId);

        check(stats.totalParticipants() == 0, "totalParticipants should be 0");
        check(stats.activePlayers() == 0, "activePlayers should be 0");
        check(stats.completedPlayers() == 0, "completedPlayers should be 0");
        check(stats.winners() == 0, "winners should be 0");
        check(stats.averageScore() == 0.0, "averageScore should be 0.0 with no participants");
    }

    private static void testGameStatsAllWinners() {
        long gameId = 0L;
        GameClock clock = StatsTestFactory.createGameClock(1L); // completed
        GameWordGroups gameGroups = createGame(gameId);
        GameRepository gameRepo = StatsTestFactory.createGameRepository(Map.of(gameId, gameGroups));
        PlayerGameRepository playerRepo = StatsTestFactory.createPlayerGameRepository();
        AccountService accountService = StatsTestFactory.createAccountService(
                Map.of("token-alice","alice", "token-bob","bob", "token-carol","carol"));

        playerRepo.save(new PlayerGame("alice", gameId, List.of(CORRECT_A, CORRECT_B, CORRECT_C)));
        playerRepo.save(new PlayerGame("bob", gameId, List.of(CORRECT_A, CORRECT_B, CORRECT_C)));
        playerRepo.save(new PlayerGame("carol", gameId, List.of(CORRECT_A, CORRECT_B, CORRECT_C)));

        StatsService statsService = StatsTestFactory.createStatsService(accountService, playerRepo, gameRepo, clock);
        GameStatsData stats = statsService.getGameStats("token-alice", gameId);

        check(stats.totalParticipants() == 3, "totalParticipants should be 3");
        check(stats.completedPlayers() == 3, "completedPlayers should be 3");
        check(stats.winners() == 3, "winners should be 3 when everyone won");
        check(stats.activePlayers() == 0, "activePlayers should be 0 for completed game");
        check(Math.abs(stats.averageScore() - 18.0) < 0.001, "averageScore should be 18.0");
    }

    private static void testGameStatsAllLosers() {
        long gameId = 0L;
        GameClock clock = StatsTestFactory.createGameClock(1L); // completed
        GameWordGroups gameGroups = createGame(gameId);
        GameRepository gameRepo = StatsTestFactory.createGameRepository(Map.of(gameId, gameGroups));
        PlayerGameRepository playerRepo = StatsTestFactory.createPlayerGameRepository();
        AccountService accountService = StatsTestFactory.createAccountService(
                Map.of("token-alice","alice", "token-bob","bob", "token-carol","carol"));

        playerRepo.save(new PlayerGame("alice", gameId, List.of(WRONG_1, WRONG_1, WRONG_1, WRONG_1)));
        playerRepo.save(new PlayerGame("bob", gameId, List.of(WRONG_1, WRONG_1, WRONG_1, WRONG_1)));
        playerRepo.save(new PlayerGame("carol", gameId, List.of(WRONG_1, WRONG_1, WRONG_1, WRONG_1)));

        StatsService statsService = StatsTestFactory.createStatsService(accountService, playerRepo, gameRepo, clock);
        GameStatsData stats = statsService.getGameStats("token-alice", gameId);

        check(stats.totalParticipants() == 3, "totalParticipants should be 3");
        check(stats.completedPlayers() == 3, "completedPlayers should be 3");
        check(stats.winners() == 0, "winners should be 0 when everyone lost");
        check(stats.activePlayers() == 0, "activePlayers should be 0 for completed game");
        check(Math.abs(stats.averageScore() - (-16.0)) < 0.001, "averageScore should be -16.0");
    }

    private static void testGameStatsActiveVsCompletedOngoing() {
        long gameId = 0L;
        GameClock clock = StatsTestFactory.createGameClock(ACTIVE_DURATION);
        GameWordGroups gameGroups = createGame(gameId);
        GameRepository gameRepo = StatsTestFactory.createGameRepository(Map.of(gameId, gameGroups));
        PlayerGameRepository playerRepo = StatsTestFactory.createPlayerGameRepository();
        AccountService accountService = StatsTestFactory.createAccountService(
                Map.of("token-alice","alice", "token-bob","bob", "token-carol","carol"));

        // Alice has already lost (4 wrong) → completed, not active
        playerRepo.save(new PlayerGame("alice", gameId, List.of(WRONG_1, WRONG_1, WRONG_1, WRONG_1)));
        // Bob is still playing (2 wrong)
        playerRepo.save(new PlayerGame("bob", gameId, List.of(WRONG_1, WRONG_1)));
        // Carol joined but hasn't guessed yet
        playerRepo.save(new PlayerGame("carol", gameId, List.of()));

        StatsService statsService = StatsTestFactory.createStatsService(accountService, playerRepo, gameRepo, clock);
        GameStatsData stats = statsService.getGameStats("token-alice", gameId);

        check(!stats.completed(), "game should be ongoing");
        check(stats.totalParticipants() == 3, "totalParticipants should be 3");
        check(stats.completedPlayers() == 1, "completedPlayers should be 1 (alice lost)");
        check(stats.activePlayers() == 2, "activePlayers should be 2 (bob, carol)");
        check(stats.winners() == 0, "winners should be 0");
    }

    private static void testGameStatsNullGameId() {
        long gameId = 0L;
        GameClock clock = StatsTestFactory.createGameClock(ACTIVE_DURATION);
        GameWordGroups gameGroups = createGame(gameId);
        GameRepository gameRepo = StatsTestFactory.createGameRepository(Map.of(gameId, gameGroups));
        PlayerGameRepository playerRepo = StatsTestFactory.createPlayerGameRepository();
        AccountService accountService = StatsTestFactory.createAccountService(Map.of("token-alice","alice"));

        playerRepo.save(new PlayerGame("alice", gameId, List.of(CORRECT_A, CORRECT_B, CORRECT_C)));

        StatsService statsService = StatsTestFactory.createStatsService(accountService, playerRepo, gameRepo, clock);
        // Null gameId should resolve to the current game
        GameStatsData stats = statsService.getGameStats("token-alice", null);

        check(stats.gameId() == gameId, "Null gameId should resolve to current game");
        check(stats.totalParticipants() == 1, "Should reflect current game stats");
    }

    private static void testLeaderboardEmptySystem() {
        GameRepository gameRepo = StatsTestFactory.createGameRepository(Map.of(0L, createGame(0L)));
        PlayerGameRepository playerRepo = StatsTestFactory.createPlayerGameRepository();
        AccountService accountService = StatsTestFactory.createAccountService(Map.of("token-alice","alice"));

        LeaderboardService leaderboardService = StatsTestFactory.createLeaderboardService(accountService, playerRepo, gameRepo);
        LeaderboardData data = leaderboardService.getLeaderboard("token-alice", null, null);

        check(data.topPlayers().isEmpty(), "Empty system should return empty topPlayers");
    }

    private static void testLeaderboardTopKBounds() {
        Map<Long, GameWordGroups> games = new HashMap<>();
        games.put(0L, createGame(0L));
        GameRepository gameRepo = StatsTestFactory.createGameRepository(games);
        PlayerGameRepository playerRepo = StatsTestFactory.createPlayerGameRepository();
        AccountService accountService = StatsTestFactory.createAccountService(
                Map.of("token-alice","alice", "token-bob","bob", "token-carol","carol"));

        playerRepo.save(new PlayerGame("alice", 0L, List.of(CORRECT_A, CORRECT_B, CORRECT_C))); // 18
        playerRepo.save(new PlayerGame("bob", 0L, List.of(CORRECT_A, CORRECT_B, CORRECT_C))); // 18
        playerRepo.save(new PlayerGame("carol", 0L, List.of(CORRECT_A, CORRECT_B, CORRECT_C))); // 18

        LeaderboardService leaderboardService = StatsTestFactory.createLeaderboardService(accountService, playerRepo, gameRepo);

        LeaderboardData top0 = leaderboardService.getLeaderboard("token-alice", null, 0);
        check(top0.topPlayers().isEmpty(), "topK=0 should return empty list");

        LeaderboardData top999 = leaderboardService.getLeaderboard("token-alice", null, 999);
        check(top999.topPlayers().size() == 3, "topK larger than player count should return all players");
    }

    private static void testLeaderboardTiedScores() {
        Map<Long, GameWordGroups> games = new HashMap<>();
        games.put(0L, createGame(0L));
        GameRepository gameRepo = StatsTestFactory.createGameRepository(games);
        PlayerGameRepository playerRepo = StatsTestFactory.createPlayerGameRepository();
        AccountService accountService = StatsTestFactory.createAccountService(
                Map.of("token-alice","alice", "token-bob","bob", "token-carol","carol"));

        playerRepo.save(new PlayerGame("alice", 0L, List.of(CORRECT_A, CORRECT_B, CORRECT_C)));
        playerRepo.save(new PlayerGame("bob", 0L, List.of(CORRECT_A, CORRECT_B, CORRECT_C)));
        playerRepo.save(new PlayerGame("carol", 0L, List.of(CORRECT_A, CORRECT_B, CORRECT_C)));

        LeaderboardService leaderboardService = StatsTestFactory.createLeaderboardService(accountService, playerRepo, gameRepo);
        LeaderboardData data = leaderboardService.getLeaderboard("token-alice", null, null);

        check(data.topPlayers().size() == 3, "All 3 players should be present");
        long distinctScores = data.topPlayers().stream().mapToInt(LeaderboardEntry::score).distinct().count();
        check(distinctScores == 1, "All players should have the same score");
        // Verify descending order (or at least non-increasing)
        for (int i = 0; i < data.topPlayers().size() - 1; i++) {
            check(data.topPlayers().get(i).score() >= data.topPlayers().get(i + 1).score(),
                    "Leaderboard should be sorted by score descending");
        }
    }

    private static void testLeaderboardNonExistentPlayer() {
        Map<Long, GameWordGroups> games = new HashMap<>();
        games.put(0L, createGame(0L));
        GameRepository gameRepo = StatsTestFactory.createGameRepository(games);
        PlayerGameRepository playerRepo = StatsTestFactory.createPlayerGameRepository();
        AccountService accountService = StatsTestFactory.createAccountService(
                Map.of("token-alice","alice", "token-bob","bob"));

        playerRepo.save(new PlayerGame("alice", 0L, List.of(CORRECT_A, CORRECT_B, CORRECT_C)));
        playerRepo.save(new PlayerGame("bob", 0L, List.of(CORRECT_A, CORRECT_B, CORRECT_C)));

        LeaderboardService leaderboardService = StatsTestFactory.createLeaderboardService(accountService, playerRepo, gameRepo);
        LeaderboardData data = leaderboardService.getLeaderboard("token-alice", "zebra", null);

        check(data.requestedPlayer() == null, "Non-existent player lookup should return null");
    }

    // ---------------------- Helper methods ----------------------

    private static boolean allZeroHistogram(Map<Integer, Integer> hist) {
        return hist.values().stream().allMatch(v -> v == 0);
    }
}