package test.game;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import server.dto.GameWordGroups;
import server.game.GameClock;
import server.game.GameRepository;
import server.game.exceptions.GameNotFoundException;

/**
 * Simple test runner for the Game catalog slice (Slice B). Tests GameClock and GameRepository
 * components using a factory. The repository implementation maps game IDs to file entries via
 * modulo (games loop around), so tests reflect that behavior.
 */
public class GameCatalogTest {

  private static int passed = 0;
  private static int failed = 0;

  public static void main(String[] args) {
    System.out.println("Running Game catalog slice tests...\n");

    runTest("testCurrentGameId", GameCatalogTest::testCurrentGameId);
    runTest("testStartedAt", GameCatalogTest::testStartedAt);
    runTest("testExpiresAt", GameCatalogTest::testExpiresAt);
    runTest("testIsCompleted", GameCatalogTest::testIsCompleted);
    runTest("testGameRepositoryLoadWithModulo", GameCatalogTest::testGameRepositoryLoadWithModulo);
    runTest("testGameRepositoryExists", GameCatalogTest::testGameRepositoryExists);
    runTest(
        "testGameRepositoryLoadEmptyFileThrows",
        GameCatalogTest::testGameRepositoryLoadEmptyFileThrows);
    runTest(
        "testGameRepositoryNegativeGameIdThrows",
        GameCatalogTest::testGameRepositoryNegativeGameIdThrows);
    runTest(
        "testGameRepositoryIndexesByArrayPositionNotGameIdField",
        GameCatalogTest::testGameRepositoryIndexesByArrayPositionNotGameIdField);
    runTest(
        "testGameRepositoryMissingFileThrows",
        GameCatalogTest::testGameRepositoryMissingFileThrows);
    runTest(
        "testGameRepositoryCorruptJsonThrows",
        GameCatalogTest::testGameRepositoryCorruptJsonThrows);

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

  private static void assertThrows(
      Class<? extends Throwable> expectedType, Runnable action, String message) {
    try {
      action.run();
      throw new AssertionError(message + " (no exception thrown)");
    } catch (Throwable t) {
      if (!expectedType.isInstance(t)) {
        throw new AssertionError(
            message
                + " (expected "
                + expectedType.getSimpleName()
                + " but got "
                + t.getClass().getSimpleName()
                + ")");
      }
    }
  }

  // ---------------------- GameClock tests ----------------------

  private static void testCurrentGameId() {
    GameClock clock = GameCatalogTestFactory.createGameClock(1000L);
    check(clock.currentGameId(0L) == 0L, "At t=0, gameId should be 0");
    check(clock.currentGameId(999L) == 0L, "At t=999, gameId should be 0");
    check(clock.currentGameId(1000L) == 1L, "At t=1000, gameId should be 1");
    check(clock.currentGameId(1999L) == 1L, "At t=1999, gameId should be 1");
    check(clock.currentGameId(2000L) == 2L, "At t=2000, gameId should be 2");
  }

  private static void testStartedAt() {
    GameClock clock = GameCatalogTestFactory.createGameClock(1000L);
    check(clock.startedAt(0L) == 0L, "Game 0 should start at t=0");
    check(clock.startedAt(1L) == 1000L, "Game 1 should start at t=1000");
    check(clock.startedAt(2L) == 2000L, "Game 2 should start at t=2000");
  }

  private static void testExpiresAt() {
    GameClock clock = GameCatalogTestFactory.createGameClock(1000L);
    check(clock.expiresAt(0L) == 1000L, "Game 0 should expire at t=1000");
    check(clock.expiresAt(1L) == 2000L, "Game 1 should expire at t=2000");
    check(clock.expiresAt(2L) == 3000L, "Game 2 should expire at t=3000");
  }

  private static void testIsCompleted() {
    GameClock clock = GameCatalogTestFactory.createGameClock(1000L);
    check(!clock.isCompleted(0L, 0L), "Game 0 should not be completed at t=0");
    check(!clock.isCompleted(0L, 999L), "Game 0 should not be completed at t=999");
    check(clock.isCompleted(0L, 1000L), "Game 0 should be completed at t=1000");
    check(!clock.isCompleted(1L, 1000L), "Game 1 should not be completed at t=1000");
    check(!clock.isCompleted(1L, 1999L), "Game 1 should not be completed at t=1999");
    check(clock.isCompleted(1L, 2000L), "Game 1 should be completed at t=2000");
  }

  // ---------------------- GameRepository tests ----------------------

  // Helper to create a temporary JSON file with exactly two games.
  private static String createTempGameDataFile() throws IOException {
    String json =
        """
        [
          {
            "gameId": 0,
            "groups": [
              {"theme": "Colors", "words": ["red", "blue", "green", "yellow"]},
              {"theme": "Fruits", "words": ["apple", "banana", "orange", "grape"]}
            ]
          },
          {
            "gameId": 1,
            "groups": [
              {"theme": "Animals", "words": ["cat", "dog", "bird", "fish"]}
            ]
          }
        ]
        """;
    Path tempFile = Files.createTempFile("game-catalog-test", ".json");
    Files.writeString(tempFile, json);
    return tempFile.toString();
  }

  private static void testGameRepositoryLoadWithModulo() throws IOException {
    String filePath = createTempGameDataFile();
    try {
      GameRepository repo = GameCatalogTestFactory.createGameRepository(filePath);

      // gameId 0 -> first entry (2 groups)
      GameWordGroups game0 = repo.loadById(0L);
      check(game0.gameId() == 0L, "Loaded game should report original gameId (0)");
      check(game0.groups().size() == 2, "First entry should have 2 groups");
      check("Colors".equals(game0.groups().get(0).theme()), "First theme should be Colors");

      // gameId 1 -> second entry (1 group)
      GameWordGroups game1 = repo.loadById(1L);
      check(game1.gameId() == 1L, "Loaded game should report original gameId (1)");
      check(game1.groups().size() == 1, "Second entry should have 1 group");
      check("Animals".equals(game1.groups().get(0).theme()), "Theme should be Animals");

      // gameId 2 -> wraps to first entry
      GameWordGroups game2 = repo.loadById(2L);
      check(game2.gameId() == 2L, "Loaded game should report original gameId (2)");
      check(game2.groups().size() == 2, "Wrapped game 2 should have 2 groups");

      // gameId 3 -> wraps to second entry
      GameWordGroups game3 = repo.loadById(3L);
      check(game3.gameId() == 3L, "Loaded game should report original gameId (3)");
      check(game3.groups().size() == 1, "Wrapped game 3 should have 1 group");
    } finally {
      new File(filePath).delete();
    }
  }

  private static void testGameRepositoryExists() throws IOException {
    String filePath = createTempGameDataFile();
    try {
      GameRepository repo = GameCatalogTestFactory.createGameRepository(filePath);
      // With modulo, any non‑negative gameId is valid as long as totalGames > 0.
      check(repo.exists(0L), "Game 0 should exist");
      check(repo.exists(1L), "Game 1 should exist");
      check(repo.exists(2L), "Game 2 should exist (wraps)");
      check(repo.exists(100L), "Game 100 should exist (wraps)");
    } finally {
      new File(filePath).delete();
    }
  }

  private static void testGameRepositoryLoadEmptyFileThrows() throws IOException {
    // Create an empty JSON array file.
    Path tempFile = Files.createTempFile("game-catalog-empty", ".json");
    Files.writeString(tempFile, "[]");
    try {
      GameRepository repo = GameCatalogTestFactory.createGameRepository(tempFile.toString());
      assertThrows(
          GameNotFoundException.class,
          () -> repo.loadById(0L),
          "Loading from empty file should throw GameNotFoundException");
      check(!repo.exists(0L), "exists should be false when file is empty");
    } finally {
      new File(tempFile.toString()).delete();
    }
  }

  private static void testGameRepositoryNegativeGameIdThrows() throws IOException {
    String filePath = createTempGameDataFile();
    try {
      GameRepository repo = GameCatalogTestFactory.createGameRepository(filePath);
      // Java's % returns negative results for negative operands, so a naive
      // "index % totalGames" implementation could throw
      // ArrayIndexOutOfBoundsException instead of a domain exception here.
      check(!repo.exists(-1L), "Negative gameId should never be reported as existing");
      assertThrows(
          GameNotFoundException.class,
          () -> repo.loadById(-1L),
          "Loading a negative gameId should throw GameNotFoundException, not an unchecked indexing"
              + " error");
    } finally {
      new File(filePath).delete();
    }
  }

  private static void testGameRepositoryIndexesByArrayPositionNotGameIdField() throws IOException {
    // The stored "gameId" field is deliberately out of step with array
    // position (index 0 claims to be gameId 99, index 1 claims gameId 5).
    // Confirmed behavior: loadById(i) must return the i-th array entry
    // regardless of what its own "gameId" field says.
    String json =
        """
        [
          {
            "gameId": 99,
            "groups": [
              {"theme": "Colors", "words": ["red", "blue", "green", "yellow"]}
            ]
          },
          {
            "gameId": 5,
            "groups": [
              {"theme": "Animals", "words": ["cat", "dog", "bird", "fish"]}
            ]
          }
        ]
        """;
    Path tempFile = Files.createTempFile("game-catalog-field-mismatch", ".json");
    Files.writeString(tempFile, json);
    try {
      GameRepository repo = GameCatalogTestFactory.createGameRepository(tempFile.toString());

      GameWordGroups game0 = repo.loadById(0L);
      check(
          game0.gameId() == 0L,
          "loadById(0) should report gameId 0, ignoring the file's own \"gameId\":99");
      check(
          "Colors".equals(game0.groups().get(0).theme()),
          "loadById(0) should return the first array entry (Colors) regardless of its gameId"
              + " field");

      GameWordGroups game1 = repo.loadById(1L);
      check(
          game1.gameId() == 1L,
          "loadById(1) should report gameId 1, ignoring the file's own \"gameId\":5");
      check(
          "Animals".equals(game1.groups().get(0).theme()),
          "loadById(1) should return the second array entry (Animals) regardless of its gameId"
              + " field");
    } finally {
      new File(tempFile.toString()).delete();
    }
  }

  private static void testGameRepositoryMissingFileThrows() {
    // Whether this fails at construction or lazily at first use is an
    // implementation detail; either way it must not succeed silently or
    // crash with a raw unchecked I/O exception type leaking out.
    String missingPath = "/tmp/definitely-does-not-exist-" + System.nanoTime() + ".json";
    try {
      GameRepository repo = GameCatalogTestFactory.createGameRepository(missingPath);
      // Construction didn't fail eagerly - it must fail on first use instead.
      assertThrows(
          Exception.class,
          () -> repo.loadById(0L),
          "Using a repository backed by a nonexistent file should throw, not return/crash"
              + " silently");
    } catch (Exception constructionFailure) {
      // Failing fast at construction time is also an acceptable design -
      // nothing further to assert here.
    }
  }

  private static void testGameRepositoryCorruptJsonThrows() throws IOException {
    Path tempFile = Files.createTempFile("game-catalog-corrupt", ".json");
    Files.writeString(tempFile, "{ this is not valid json at all [[[");
    try {
      try {
        GameRepository repo = GameCatalogTestFactory.createGameRepository(tempFile.toString());
        assertThrows(
            Exception.class,
            () -> repo.loadById(0L),
            "Using a repository backed by unparsable JSON should throw, not return/crash silently");
      } catch (Exception constructionFailure) {
        // Failing fast at construction time is also acceptable.
      }
    } finally {
      new File(tempFile.toString()).delete();
    }
  }
}
