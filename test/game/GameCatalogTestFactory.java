package test.game;

import server.game.FileGameRepository;
import server.game.GameClock;
import server.game.GameClockImpl;
import server.game.GameRepository;

/**
 * Factory for creating test instances of the Game catalog components (Slice B). Centralises
 * concrete class names and configuration.
 */
public class GameCatalogTestFactory {

  public static final long DEFAULT_GAME_DURATION_MILLIS = 60_000L; // 1 minute

  public static GameClock createGameClock() {
    return new GameClockImpl(DEFAULT_GAME_DURATION_MILLIS);
  }

  public static GameClock createGameClock(long gameDurationMillis) {
    return new GameClockImpl(gameDurationMillis);
  }

  public static GameRepository createGameRepository(String gameDataFilePath) {
    return new FileGameRepository(gameDataFilePath);
  }
}
