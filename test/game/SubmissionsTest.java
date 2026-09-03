package test.game;

import server.account.AccountService;
import server.account.exceptions.InvalidTokenException;
import server.dto.PlayerGame;
import server.game.GameClock;
import server.game.GameRepository;
import server.game.PlayerGameRepository;
import server.game.ProposalService;
import server.game.exceptions.*;
import shared.dto.GameInfoData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Test runner for Proposal submission & per-player game state slice (Slice C).
 * Tests ProposalService and PlayerGameRepository using the real implementations.
 */
public class SubmissionsTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("Running Proposal submission slice tests...\n");

        runTest("testPlayerGameRepositoryFindOrCreateNew", SubmissionsTest::testPlayerGameRepositoryFindOrCreateNew);
        runTest("testPlayerGameRepositoryFindOrCreateExisting", SubmissionsTest::testPlayerGameRepositoryFindOrCreateExisting);
        runTest("testPlayerGameRepositorySaveAndFindByGame", SubmissionsTest::testPlayerGameRepositorySaveAndFindByGame);
        runTest("testPlayerGameRepositoryFindByUsername", SubmissionsTest::testPlayerGameRepositoryFindByUsername);

        runTest("testSubmitProposalCorrectGroup", SubmissionsTest::testSubmitProposalCorrectGroup);
        runTest("testSubmitProposalWrongGroup", SubmissionsTest::testSubmitProposalWrongGroup);
        runTest("testSubmitProposalMalformed", SubmissionsTest::testSubmitProposalMalformed);
        runTest("testSubmitProposalUnknownWords", SubmissionsTest::testSubmitProposalUnknownWords);
        runTest("testSubmitProposalAlreadyGrouped", SubmissionsTest::testSubmitProposalAlreadyGrouped);
        runTest("testSubmitProposalInvalidToken", SubmissionsTest::testSubmitProposalInvalidToken);
        runTest("testGetGameInfoOngoing", SubmissionsTest::testGetGameInfoOngoing);
        runTest("testGetGameInfoCompleted", SubmissionsTest::testGetGameInfoCompleted);
        runTest("testGetGameInfoInvalidGameId", SubmissionsTest::testGetGameInfoInvalidGameId);

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

    // ---------------------- Test setup helpers ----------------------

    private static String createTestGameDataFile() throws IOException {
        String json = """
                [
                  {
                    "gameId": 0,
                    "groups": [
                      {"theme": "Colors", "words": ["red", "blue", "green", "yellow"]},
                      {"theme": "Fruits", "words": ["apple", "banana", "orange", "grape"]},
                      {"theme": "Animals", "words": ["cat", "dog", "bird", "fish"]},
                      {"theme": "Shapes", "words": ["circle", "square", "triangle", "star"]}
                    ]
                  }
                ]
                """;
        Path tempFile = Files.createTempFile("submissions-test", ".json");
        Files.writeString(tempFile, json);
        return tempFile.toString();
    }

    private static TestContext createTestContext() throws IOException {
        String gameFile = createTestGameDataFile();
        // Use Long.MAX_VALUE so that currentGameId == 0 for all test executions.
        GameClock clock = SubmissionsTestFactory.createGameClock(Long.MAX_VALUE);
        GameRepository gameRepo = SubmissionsTestFactory.createGameRepository(gameFile);
        PlayerGameRepository playerGames = SubmissionsTestFactory.createPlayerGameRepository();
        AccountService accountService = SubmissionsTestFactory.createStubAccountService();
        ProposalService proposalService = SubmissionsTestFactory.createProposalService(
                accountService, clock, gameRepo, playerGames);
        return new TestContext(clock, gameRepo, playerGames, accountService, proposalService, gameFile);
    }

    private record TestContext(
            GameClock clock,
            GameRepository gameRepo,
            PlayerGameRepository playerGames,
            AccountService accountService,
            ProposalService proposalService,
            String gameFile
    ) {}

    // ---------------------- PlayerGameRepository tests ----------------------

    private static void testPlayerGameRepositoryFindOrCreateNew() {
        PlayerGameRepository repo = SubmissionsTestFactory.createPlayerGameRepository();
        PlayerGame pg = repo.findOrCreate("alice", 1L);
        check("alice".equals(pg.username()), "Username should be alice");
        check(pg.gameId() == 1L, "Game ID should be 1");
        check(pg.proposals().isEmpty(), "New PlayerGame should have empty proposals");
    }

    private static void testPlayerGameRepositoryFindOrCreateExisting() {
        PlayerGameRepository repo = SubmissionsTestFactory.createPlayerGameRepository();
        PlayerGame first = repo.findOrCreate("bob", 2L);
        repo.save(first);
        PlayerGame second = repo.findOrCreate("bob", 2L);
        check(first == second, "findOrCreate should return the same instance if it exists");
        check(second.proposals().isEmpty(), "Proposals should still be empty");
    }

    private static void testPlayerGameRepositorySaveAndFindByGame() {
        PlayerGameRepository repo = SubmissionsTestFactory.createPlayerGameRepository();
        PlayerGame pg1 = repo.findOrCreate("alice", 1L);
        repo.save(pg1);
        PlayerGame pg2 = repo.findOrCreate("bob", 1L);
        repo.save(pg2);
        PlayerGame pg3 = repo.findOrCreate("alice", 2L);
        repo.save(pg3);

        List<PlayerGame> game1Players = repo.findByGame(1L);
        check(game1Players.size() == 2, "Game 1 should have 2 players");
        check(game1Players.contains(pg1) && game1Players.contains(pg2), "Both alice and bob should be present");

        List<PlayerGame> game2Players = repo.findByGame(2L);
        check(game2Players.size() == 1 && game2Players.contains(pg3), "Game 2 should have only alice");
    }

    private static void testPlayerGameRepositoryFindByUsername() {
        PlayerGameRepository repo = SubmissionsTestFactory.createPlayerGameRepository();
        PlayerGame pg1 = repo.findOrCreate("alice", 1L);
        repo.save(pg1);
        PlayerGame pg2 = repo.findOrCreate("alice", 2L);
        repo.save(pg2);
        PlayerGame pg3 = repo.findOrCreate("bob", 1L);
        repo.save(pg3);

        List<PlayerGame> aliceGames = repo.findPlayerGameByUsername("alice");
        check(aliceGames.size() == 2, "Alice should have 2 games");
        check(aliceGames.contains(pg1) && aliceGames.contains(pg2), "Both games should be present");

        List<PlayerGame> bobGames = repo.findPlayerGameByUsername("bob");
        check(bobGames.size() == 1 && bobGames.contains(pg3), "Bob should have 1 game");
    }

    // ---------------------- ProposalService tests ----------------------

    private static void testSubmitProposalCorrectGroup() throws IOException {
        TestContext ctx = createTestContext();
        try {
            ProposalService service = ctx.proposalService();
            // Since current game is 0 (due to Long.MAX_VALUE duration), we can use 0 directly.
            GameInfoData info = service.submitProposal("token1", 0L, List.of("red", "blue", "green", "yellow"));
            check(info.correctGuesses().size() == 1, "Should have 1 correct guess");
            check(info.correctGuesses().get(0).equals(Set.of("red", "blue", "green", "yellow")),
                    "Correct guess should contain the correct words");
            check(info.wrongGuesses().isEmpty(), "No wrong guesses yet");
            check(info.correctGroups() == null, "Game is ongoing so correctGroups should be null");
        } finally {
            new File(ctx.gameFile()).delete();
        }
    }

    private static void testSubmitProposalWrongGroup() throws IOException {
        TestContext ctx = createTestContext();
        try {
            ProposalService service = ctx.proposalService();
            GameInfoData info = service.submitProposal("token1", 0L, List.of("red", "blue", "green", "apple"));
            check(info.correctGuesses().isEmpty(), "No correct guesses");
            check(info.wrongGuesses().size() == 1, "Should have 1 wrong guess");
            check(info.wrongGuesses().get(0).equals(Set.of("red", "blue", "green", "apple")),
                    "Wrong guess should contain the submitted words");
        } finally {
            new File(ctx.gameFile()).delete();
        }
    }

    private static void testSubmitProposalMalformed() throws IOException {
        TestContext ctx = createTestContext();
        try {
            ProposalService service = ctx.proposalService();
            assertThrows(MalformedProposalException.class,
                    () -> service.submitProposal("token1", 0L, List.of("red", "blue", "green")),
                    "Submitting 3 words should throw MalformedProposalException");
        } finally {
            new File(ctx.gameFile()).delete();
        }
    }

    private static void testSubmitProposalUnknownWords() throws IOException {
        TestContext ctx = createTestContext();
        try {
            ProposalService service = ctx.proposalService();
            assertThrows(UnknownWordsInProposalException.class,
                    () -> service.submitProposal("token1", 0L, List.of("red", "blue", "green", "purple")),
                    "Submitting a word not in the game should throw UnknownWordsInProposalException");
        } finally {
            new File(ctx.gameFile()).delete();
        }
    }

    private static void testSubmitProposalAlreadyGrouped() throws IOException {
        TestContext ctx = createTestContext();
        try {
            ProposalService service = ctx.proposalService();
            // Submit correct group first
            service.submitProposal("token1", 0L, List.of("red", "blue", "green", "yellow"));
            // Attempt to submit same group again
            assertThrows(WordsAlreadyGroupedException.class,
                    () -> service.submitProposal("token1", 0L, List.of("red", "blue", "green", "yellow")),
                    "Submitting already grouped words should throw WordsAlreadyGroupedException");
        } finally {
            new File(ctx.gameFile()).delete();
        }
    }

    private static void testSubmitProposalInvalidToken() throws IOException {
        TestContext ctx = createTestContext();
        try {
            ProposalService service = ctx.proposalService();
            assertThrows(InvalidTokenException.class,
                    () -> service.submitProposal("invalid_token", 0L, List.of("red", "blue", "green", "yellow")),
                    "Invalid token should throw InvalidTokenException");
        } finally {
            new File(ctx.gameFile()).delete();
        }
    }

    private static void testGetGameInfoOngoing() throws IOException {
        TestContext ctx = createTestContext();
        try {
            ProposalService service = ctx.proposalService();
            GameInfoData info = service.getGameInfo("token1", 0L);
            check(info.gameId() == 0L, "Current game ID should be 0");
            check(info.correctGuesses().isEmpty() && info.wrongGuesses().isEmpty(),
                    "No guesses yet");
            check(info.words().size() == 16, "There should be 16 words");
            check(info.correctGroups() == null, "Ongoing game should not include correct groups");

            // Submit one correct guess and retrieve info again
            service.submitProposal("token1", 0L, List.of("red", "blue", "green", "yellow"));
            info = service.getGameInfo("token1", 0L);
            check(info.correctGuesses().size() == 1, "One correct guess expected");
            check(info.correctGroups() == null, "Ongoing game still should not include correct groups");
        } finally {
            new File(ctx.gameFile()).delete();
        }
    }

    private static void testGetGameInfoCompleted() throws IOException {
        // Use a clock with very short duration so game 0 is already completed
        String gameFile = createTestGameDataFile();
        GameClock clock = SubmissionsTestFactory.createGameClock(1L); // 1 ms duration
        GameRepository gameRepo = SubmissionsTestFactory.createGameRepository(gameFile);
        PlayerGameRepository playerGames = SubmissionsTestFactory.createPlayerGameRepository();
        AccountService accountService = SubmissionsTestFactory.createStubAccountService();
        ProposalService service = SubmissionsTestFactory.createProposalService(
                accountService, clock, gameRepo, playerGames);

        try {
            GameInfoData info = service.getGameInfo("token1", 0L);
            check(info.correctGroups() != null, "Completed game should include correct groups");
            check(info.correctGroups().size() == 4, "There should be 4 correct groups");
            check(info.correctGroups().get(0).equals(List.of("red", "blue", "green", "yellow")),
                    "First group should be Colors");
        } finally {
            new File(gameFile).delete();
        }
    }

    private static void testGetGameInfoInvalidGameId() throws IOException {
        TestContext ctx = createTestContext();
        try {
            ProposalService service = ctx.proposalService();
            // Negative gameId is not valid in the time-based model (maps to negative index).
            assertThrows(GameNotFoundException.class,
                    () -> service.getGameInfo("token1", -1L),
                    "Requesting a negative game ID should throw GameNotFoundException");
        } finally {
            new File(ctx.gameFile()).delete();
        }
    }
}