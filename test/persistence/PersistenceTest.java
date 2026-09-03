package test.persistence;

import server.account.AccountRepository;
import server.game.PlayerGameRepository;
import server.persistence.PersistenceService;
import server.dto.Account;
import server.dto.PlayerGame;
import server.dto.Proposal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Tests for the Persistence slice (Slice F).
 * Verifies save/load round‑trips for accounts and player games,
 * plus handling of empty snapshots and non‑existent snapshot files.
 */
public class PersistenceTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("Running Persistence slice tests...\n");

        runTest("testSaveAndLoadAccounts", PersistenceTest::testSaveAndLoadAccounts);
        runTest("testSaveAndLoadPlayerGames", PersistenceTest::testSaveAndLoadPlayerGames);
        runTest("testLoadNonExistentSnapshot", PersistenceTest::testLoadNonExistentSnapshot);

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

    // ---------------------- Helper assertions ----------------------

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // ---------------------- Test methods ----------------------

    private static void testSaveAndLoadAccounts() throws IOException {
        Path tempDir = Files.createTempDirectory("persistence-test-accounts");
        try {
            PersistenceService persistence = PersistenceTestFactory.createPersistenceService(tempDir);
            AccountRepository source = PersistenceTestFactory.createAccountRepository();

            // Populate source
            source.save(new Account("alice", "hashA"));
            source.save(new Account("bob", "hashB"));
            source.save(new Account("charlie", "hashC"));

            // Save snapshot
            persistence.saveSnapshot(source, PersistenceTestFactory.createPlayerGameRepository());

            // Load into fresh repositories
            AccountRepository loadedAccounts = PersistenceTestFactory.createAccountRepository();
            PlayerGameRepository loadedGames = PersistenceTestFactory.createPlayerGameRepository();
            persistence.loadSnapshot(loadedAccounts, loadedGames);

            // Verify
            check(loadedAccounts.existsByUsername("alice"), "alice should exist after load");
            check(loadedAccounts.existsByUsername("bob"), "bob should exist after load");
            check(loadedAccounts.existsByUsername("charlie"), "charlie should exist after load");
            check(!loadedAccounts.existsByUsername("dave"), "dave should not exist");

            Account alice = loadedAccounts.findAccountByUsername("alice").orElseThrow();
            check("hashA".equals(alice.passwordHash()), "alice's password hash should match");
        } finally {
            // Cleanup
            Files.walk(tempDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> p.toFile().delete());
        }
    }

    private static void testSaveAndLoadPlayerGames() throws IOException {
        Path tempDir = Files.createTempDirectory("persistence-test-playergames");
        try {
            PersistenceService persistence = PersistenceTestFactory.createPersistenceService(tempDir);
            PlayerGameRepository source = PersistenceTestFactory.createPlayerGameRepository();

            // Create some player games with proposals
            PlayerGame pg1 = source.findOrCreate("alice", 1L);
            pg1 = new PlayerGame(pg1.username(), pg1.gameId(), List.of(
                    new Proposal(Set.of("word1", "word2", "word3", "word4")),
                    new Proposal(Set.of("word5", "word6", "word7", "word8"))
            ));
            source.save(pg1);

            PlayerGame pg2 = source.findOrCreate("bob", 1L);
            pg2 = new PlayerGame(pg2.username(), pg2.gameId(), List.of(
                    new Proposal(Set.of("word9", "word10", "word11", "word12"))
            ));
            source.save(pg2);

            PlayerGame pg3 = source.findOrCreate("alice", 2L);
            pg3 = new PlayerGame(pg3.username(), pg3.gameId(), List.of());
            source.save(pg3);

            // Save snapshot
            persistence.saveSnapshot(PersistenceTestFactory.createAccountRepository(), source);

            // Load into fresh repositories
            AccountRepository loadedAccounts = PersistenceTestFactory.createAccountRepository();
            PlayerGameRepository loadedGames = PersistenceTestFactory.createPlayerGameRepository();
            persistence.loadSnapshot(loadedAccounts, loadedGames);

            // Verify loaded data
            List<PlayerGame> aliceGames = loadedGames.findPlayerGameByUsername("alice");
            check(aliceGames.size() == 2, "Alice should have 2 games");
            check(aliceGames.stream().anyMatch(pg -> pg.gameId() == 1L && pg.proposals().size() == 2),
                    "Alice's game 1 should have 2 proposals");
            check(aliceGames.stream().anyMatch(pg -> pg.gameId() == 2L && pg.proposals().isEmpty()),
                    "Alice's game 2 should have no proposals");

            List<PlayerGame> bobGames = loadedGames.findPlayerGameByUsername("bob");
            check(bobGames.size() == 1, "Bob should have 1 game");
            check(bobGames.get(0).gameId() == 1L && bobGames.get(0).proposals().size() == 1,
                    "Bob's game 1 should have 1 proposal");

            // Check game lookup
            List<PlayerGame> game1Players = loadedGames.findByGame(1L);
            check(game1Players.size() == 2, "Game 1 should have 2 players");
        } finally {
            Files.walk(tempDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> p.toFile().delete());
        }
    }

    private static void testLoadNonExistentSnapshot() throws IOException {
        Path tempDir = Files.createTempDirectory("persistence-test-nonexistent");
        try {
            PersistenceService persistence = PersistenceTestFactory.createPersistenceService(tempDir);

            AccountRepository accounts = PersistenceTestFactory.createAccountRepository();
            PlayerGameRepository games = PersistenceTestFactory.createPlayerGameRepository();

            // Loading from a directory without snapshot files should not throw
            // and should leave the repositories empty.
            persistence.loadSnapshot(accounts, games);

            check(!accounts.existsByUsername("anyone"), "Accounts should be empty after loading missing snapshot");
            check(games.findAllUsernames().isEmpty(), "Player games should be empty after loading missing snapshot");
        } finally {
            Files.walk(tempDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> p.toFile().delete());
        }
    }
}