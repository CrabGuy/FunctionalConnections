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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
        runTest("testSubmitProposalPartialOverlapAlreadyGrouped",
                SubmissionsTest::testSubmitProposalPartialOverlapAlreadyGrouped);
        runTest("testSubmitProposalDuplicateWordsWithinProposal",
                SubmissionsTest::testSubmitProposalDuplicateWordsWithinProposal);
        runTest("testSubmitProposalInvalidToken", SubmissionsTest::testSubmitProposalInvalidToken);
        runTest("testSubmitProposalAfterFourMistakesThrows",
                SubmissionsTest::testSubmitProposalAfterFourMistakesThrows);
        runTest("testSubmitProposalAfterThreeCorrectWinsAndBlocksFurtherSubmissions",
                SubmissionsTest::testSubmitProposalAfterThreeCorrectWinsAndBlocksFurtherSubmissions);
        runTest("testGetGameInfoOngoing", SubmissionsTest::testGetGameInfoOngoing);
        runTest("testGetGameInfoCompleted", SubmissionsTest::testGetGameInfoCompleted);
        runTest("testGetGameInfoInvalidGameId", SubmissionsTest::testGetGameInfoInvalidGameId);
        runTest("testGetGameInfoFutureGameIdThrows", SubmissionsTest::testGetGameInfoFutureGameIdThrows);
        runTest("testGetGameInfoWordsShuffledConsistently", SubmissionsTest::testGetGameInfoWordsShuffledConsistently);
        runTest("testGetGameInfoWordsShuffledDoesNotRevealGroupOrder",
                SubmissionsTest::testGetGameInfoWordsShuffledDoesNotRevealGroupOrder);
        runTest("testGetGameInfoAutoJoinsPlayer", SubmissionsTest::testGetGameInfoAutoJoinsPlayer);
        runTest("testProposalsAreIsolatedPerPlayer", SubmissionsTest::testProposalsAreIsolatedPerPlayer);
        runTest("testConcurrentFindOrCreateSamePlayerGame",
                SubmissionsTest::testConcurrentFindOrCreateSamePlayerGame);
        runTest("testConcurrentSubmitProposalTwoDistinctPlayers",
                SubmissionsTest::testConcurrentSubmitProposalTwoDistinctPlayers);

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

            // Requirements.md: malformed proposals must NOT count as mistakes.
            GameInfoData info = service.getGameInfo("token1", 0L);
            check(info.wrongGuesses().isEmpty(),
                    "A malformed (wrong word count) proposal must not increment the mistake count");
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

            GameInfoData info = service.getGameInfo("token1", 0L);
            check(info.wrongGuesses().isEmpty(),
                    "A proposal referencing a word outside the game must not increment the mistake count");
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

            GameInfoData info = service.getGameInfo("token1", 0L);
            check(info.correctGuesses().size() == 1,
                    "Resubmitting an already-solved group must not add a second correct guess");
            check(info.wrongGuesses().isEmpty(),
                    "Resubmitting an already-solved group must not count as a mistake");
        } finally {
            new File(ctx.gameFile()).delete();
        }
    }

    private static void testSubmitProposalPartialOverlapAlreadyGrouped() throws IOException {
        TestContext ctx = createTestContext();
        try {
            ProposalService service = ctx.proposalService();
            // Solve Colors first, so "red" becomes an already-grouped word.
            service.submitProposal("token1", 0L, List.of("red", "blue", "green", "yellow"));

            // Mix one already-grouped word ("red") with three fresh, ungrouped
            // words from a different theme. This must be rejected the same way
            // as resubmitting a whole solved group - it references a claimed
            // word - and must not be silently scored as a wrong guess.
            assertThrows(WordsAlreadyGroupedException.class,
                    () -> service.submitProposal("token1", 0L, List.of("red", "apple", "banana", "orange")),
                    "A proposal mixing an already-grouped word with fresh words should throw WordsAlreadyGroupedException");

            GameInfoData info = service.getGameInfo("token1", 0L);
            check(info.wrongGuesses().isEmpty(),
                    "A partial-overlap-with-already-grouped proposal must not count as a mistake");
        } finally {
            new File(ctx.gameFile()).delete();
        }
    }

    private static void testSubmitProposalDuplicateWordsWithinProposal() throws IOException {
        TestContext ctx = createTestContext();
        try {
            ProposalService service = ctx.proposalService();
            // Only 3 distinct words repeated to fill 4 slots - not a valid
            // 4-distinct-word proposal, so this should be malformed rather than
            // silently treated as a (wrong) 3-word guess.
            assertThrows(MalformedProposalException.class,
                    () -> service.submitProposal("token1", 0L, List.of("red", "red", "blue", "green")),
                    "A proposal with a repeated word should throw MalformedProposalException");

            GameInfoData info = service.getGameInfo("token1", 0L);
            check(info.wrongGuesses().isEmpty(),
                    "A proposal with duplicate words must not count as a mistake");
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

    private static void testSubmitProposalAfterFourMistakesThrows() throws IOException {
        TestContext ctx = createTestContext();
        try {
            ProposalService service = ctx.proposalService();
            // 4 distinct wrong-but-valid, not-yet-grouped proposals.
            service.submitProposal("token1", 0L, List.of("red", "blue", "green", "apple"));
            service.submitProposal("token1", 0L, List.of("yellow", "banana", "orange", "grape"));
            service.submitProposal("token1", 0L, List.of("cat", "dog", "bird", "circle"));
            service.submitProposal("token1", 0L, List.of("fish", "square", "triangle", "star"));

            GameInfoData afterFour = service.getGameInfo("token1", 0L);
            check(afterFour.wrongGuesses().size() == 4, "Player should have exactly 4 wrong guesses");

            // Requirements.md: max 4 wrong proposals per game -> player has lost.
            // A 5th attempt, even with a genuinely correct group, must be rejected.
            assertThrows(PlayerAlreadyCompletedGameException.class,
                    () -> service.submitProposal("token1", 0L, List.of("red", "blue", "green", "yellow")),
                    "Submitting a proposal after 4 mistakes should throw PlayerAlreadyCompletedGameException");

            GameInfoData after = service.getGameInfo("token1", 0L);
            check(after.wrongGuesses().size() == 4, "Mistake count must not change after the game is already lost");
            check(after.correctGuesses().isEmpty(),
                    "The rejected post-loss proposal must not be scored as correct either");
        } finally {
            new File(ctx.gameFile()).delete();
        }
    }

    private static void testSubmitProposalAfterThreeCorrectWinsAndBlocksFurtherSubmissions() throws IOException {
        TestContext ctx = createTestContext();
        try {
            ProposalService service = ctx.proposalService();
            service.submitProposal("token1", 0L, List.of("red", "blue", "green", "yellow"));
            service.submitProposal("token1", 0L, List.of("apple", "banana", "orange", "grape"));
            service.submitProposal("token1", 0L, List.of("cat", "dog", "bird", "fish"));

            // Clarified design decision: 3 correct groups = won & completed.
            // The 4th group is implied - the player does NOT need to (and
            // should not be able to) submit it or anything else afterward.
            GameInfoData afterThree = service.getGameInfo("token1", 0L);
            check(afterThree.correctGuesses().size() == 3,
                    "Exactly 3 correct guesses should be recorded - the 4th group is implied, not auto-added");

            assertThrows(PlayerAlreadyCompletedGameException.class,
                    () -> service.submitProposal("token1", 0L, List.of("circle", "square", "triangle", "star")),
                    "Submitting the (implied) 4th group after winning should throw PlayerAlreadyCompletedGameException");

            GameInfoData after = service.getGameInfo("token1", 0L);
            check(after.correctGuesses().size() == 3,
                    "Correct-guess count must stay at 3 after the win; the 4th group is never explicitly added");
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

    private static void testGetGameInfoFutureGameIdThrows() throws IOException {
        // Clarified design decision: a gameId that is arithmetically valid
        // (loadById could compute it via modulo) but hasn't been reached yet
        // by the clock is still GameNotFoundException - "could be calculated
        // but doesn't exist yet".
        TestContext ctx = createTestContext();
        try {
            ProposalService service = ctx.proposalService();
            // This context's clock uses Long.MAX_VALUE duration, so
            // currentGameId(now) is always 0 - gameId 1 is therefore always
            // "in the future" relative to it.
            assertThrows(GameNotFoundException.class,
                    () -> service.getGameInfo("token1", 1L),
                    "Requesting a game ID beyond the current game should throw GameNotFoundException");
        } finally {
            new File(ctx.gameFile()).delete();
        }
    }

    private static void testGetGameInfoWordsShuffledConsistently() throws IOException {
        TestContext ctx = createTestContext();
        try {
            ProposalService service = ctx.proposalService();
            // NOTES.md: words are shuffled using gameId as seed, so repeated
            // requests for the same game must return the same order.
            GameInfoData first = service.getGameInfo("token1", 0L);
            GameInfoData second = service.getGameInfo("token1", 0L);
            check(first.words().equals(second.words()),
                    "Word order must be deterministic/consistent across repeated requests for the same gameId");
        } finally {
            new File(ctx.gameFile()).delete();
        }
    }

    private static void testGetGameInfoWordsShuffledDoesNotRevealGroupOrder() throws IOException {
        TestContext ctx = createTestContext();
        try {
            ProposalService service = ctx.proposalService();
            GameInfoData info = service.getGameInfo("token1", 0L);

            check(info.words().size() == 16, "There should be 16 words");
            check(new HashSet<>(info.words()).equals(Set.of(
                            "red", "blue", "green", "yellow",
                            "apple", "banana", "orange", "grape",
                            "cat", "dog", "bird", "fish",
                            "circle", "square", "triangle", "star")),
                    "Shuffled words must be exactly the 16 game words, no more, no less, no duplicates");

            // Requirements.md: the server must send words "without revealing
            // groupings". If the words come back in exactly the same order as
            // the four groups are defined in the data file, the grouping is
            // trivially visible and the shuffle isn't doing its job.
            List<String> unshuffledGroupOrder = List.of(
                    "red", "blue", "green", "yellow",
                    "apple", "banana", "orange", "grape",
                    "cat", "dog", "bird", "fish",
                    "circle", "square", "triangle", "star");
            check(!info.words().equals(unshuffledGroupOrder),
                    "Word order must not simply mirror the group definitions verbatim, or it reveals the groupings");
        } finally {
            new File(ctx.gameFile()).delete();
        }
    }

    private static void testGetGameInfoAutoJoinsPlayer() throws IOException {
        TestContext ctx = createTestContext();
        try {
            // NOTES.md: any operation that gives a player information about the
            // game (including just requesting game info) should be treated as
            // "playing", creating an empty PlayerGame entry - not just submitProposal.
            check(ctx.playerGames().findByGame(0L).isEmpty(),
                    "Sanity check: no players should be tracked for game 0 before anyone interacts with it");

            ctx.proposalService().getGameInfo("token1", 0L);

            List<PlayerGame> playersInGame = ctx.playerGames().findByGame(0L);
            check(playersInGame.size() == 1,
                    "Requesting game info for a brand-new player should create an (empty) PlayerGame entry");
            check(playersInGame.get(0).proposals().isEmpty(),
                    "The auto-created entry should have no proposals yet - viewing isn't guessing");
        } finally {
            new File(ctx.gameFile()).delete();
        }
    }

    private static void testProposalsAreIsolatedPerPlayer() throws IOException {
        // token1 -> alice, token2 -> bob (per SubmissionsTestFactory's stub).
        TestContext ctx = createTestContext();
        try {
            ProposalService service = ctx.proposalService();

            // alice solves Colors and gets one wrong guess.
            service.submitProposal("token1", 0L, List.of("red", "blue", "green", "yellow"));
            service.submitProposal("token1", 0L, List.of("cat", "dog", "bird", "circle"));

            // bob has done nothing yet in the same game.
            GameInfoData bobInfo = service.getGameInfo("token2", 0L);
            check(bobInfo.correctGuesses().isEmpty(),
                    "bob's correct guesses must not include alice's solved group");
            check(bobInfo.wrongGuesses().isEmpty(),
                    "bob's wrong guesses must not include alice's mistake");

            // bob solves Fruits independently.
            service.submitProposal("token2", 0L, List.of("apple", "banana", "orange", "grape"));

            GameInfoData aliceInfo = service.getGameInfo("token1", 0L);
            check(aliceInfo.correctGuesses().size() == 1,
                    "alice's correct guesses must still only reflect her own group (Colors), not bob's (Fruits)");
            check(aliceInfo.wrongGuesses().size() == 1, "alice's mistake count must be unaffected by bob's actions");

            GameInfoData bobInfoAfter = service.getGameInfo("token2", 0L);
            check(bobInfoAfter.correctGuesses().size() == 1,
                    "bob's correct guesses must reflect only his own group (Fruits)");
            check(bobInfoAfter.wrongGuesses().isEmpty(), "bob has made no wrong guesses of his own");

            // Both players should be tracked separately in the repository.
            List<PlayerGame> playersInGame = ctx.playerGames().findByGame(0L);
            check(playersInGame.size() == 2, "Both alice and bob should have independent PlayerGame entries");
        } finally {
            new File(ctx.gameFile()).delete();
        }
    }

    // ---------------------- Concurrency tests ----------------------
    // Mirrors the same check-then-act concern raised for AccountRepository:
    // findOrCreate must not create duplicate PlayerGame entries for the same
    // (username, gameId) pair when called concurrently.

    private static void testConcurrentFindOrCreateSamePlayerGame() throws InterruptedException {
        PlayerGameRepository repo = SubmissionsTestFactory.createPlayerGameRepository();

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threadCount);
        List<PlayerGame> results = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> unexpected = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    results.add(repo.findOrCreate("dana", 5L));
                } catch (Throwable t) {
                    unexpected.add(t);
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown();
        boolean finished = doneGate.await(10, TimeUnit.SECONDS);
        executor.shutdownNow();

        check(finished, "All findOrCreate threads should complete within the timeout");
        check(unexpected.isEmpty(), "No exceptions expected from concurrent findOrCreate: " + unexpected);
        check(results.size() == threadCount, "Every thread should get a result");

        PlayerGame first = results.get(0);
        for (PlayerGame pg : results) {
            check(pg == first,
                    "Every concurrent findOrCreate(\"dana\", 5L) call should return the SAME instance "
                            + "(a check-then-act race would create duplicates)");
        }

        List<PlayerGame> stored = repo.findByGame(5L);
        check(stored.size() == 1,
                "Repository should end up with exactly one PlayerGame entry for (dana, 5), got " + stored.size());
    }

    private static void testConcurrentSubmitProposalTwoDistinctPlayers() throws IOException, InterruptedException {
        // token1 -> alice, token2 -> bob. Both hammer the same game
        // concurrently with different (correct) proposals, exercising real
        // concurrent access through ProposalService rather than just the
        // repository directly.
        TestContext ctx = createTestContext();
        try {
            ProposalService service = ctx.proposalService();

            int repetitions = 15;
            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch startGate = new CountDownLatch(1);
            CountDownLatch doneGate = new CountDownLatch(2);
            List<Throwable> unexpected = Collections.synchronizedList(new ArrayList<>());

            executor.submit(() -> {
                try {
                    startGate.await();
                    for (int i = 0; i < repetitions; i++) {
                        // alice repeatedly re-attempts her own already-solved
                        // group; only the first should count, the rest should
                        // throw WordsAlreadyGroupedException (swallowed here -
                        // we're only checking for crashes/corruption).
                        try {
                            service.submitProposal("token1", 0L, List.of("red", "blue", "green", "yellow"));
                        } catch (WordsAlreadyGroupedException expected) {
                            // fine, expected after the first success
                        }
                    }
                } catch (Throwable t) {
                    unexpected.add(t);
                } finally {
                    doneGate.countDown();
                }
            });

            executor.submit(() -> {
                try {
                    startGate.await();
                    for (int i = 0; i < repetitions; i++) {
                        try {
                            service.submitProposal("token2", 0L, List.of("apple", "banana", "orange", "grape"));
                        } catch (WordsAlreadyGroupedException expected) {
                            // fine, expected after the first success
                        }
                    }
                } catch (Throwable t) {
                    unexpected.add(t);
                } finally {
                    doneGate.countDown();
                }
            });

            startGate.countDown();
            boolean finished = doneGate.await(10, TimeUnit.SECONDS);
            executor.shutdownNow();

            check(finished, "Both player threads should complete within the timeout");
            check(unexpected.isEmpty(), "No unexpected exceptions from concurrent two-player submissions: " + unexpected);

            GameInfoData aliceInfo = service.getGameInfo("token1", 0L);
            check(aliceInfo.correctGuesses().size() == 1,
                    "alice should have exactly 1 correct guess despite repeated concurrent resubmissions, got "
                            + aliceInfo.correctGuesses().size());

            GameInfoData bobInfo = service.getGameInfo("token2", 0L);
            check(bobInfo.correctGuesses().size() == 1,
                    "bob should have exactly 1 correct guess despite repeated concurrent resubmissions, got "
                            + bobInfo.correctGuesses().size());
        } finally {
            new File(ctx.gameFile()).delete();
        }
    }
}