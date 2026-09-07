package server.persistence;

import java.io.IOException;
import server.account.AccountRepository;
import server.game.PlayerGameRepository;

/** Defines persistence operations for saving and loading snapshots of accounts and player games. */
public interface PersistenceService {

  /**
   * Saves the current state of accounts and player games to persistent storage.
   *
   * @param accounts the account repository
   * @param playerGames the player game repository
   * @throws IOException if an I/O error occurs
   */
  void saveSnapshot(AccountRepository accounts, PlayerGameRepository playerGames)
      throws IOException;

  /**
   * Loads the snapshot into the provided repositories.
   *
   * @param accounts the account repository to populate
   * @param playerGames the player game repository to populate
   * @throws IOException if an I/O error occurs
   */
  void loadSnapshot(AccountRepository accounts, PlayerGameRepository playerGames)
      throws IOException;

  /**
   * Schedules periodic snapshot saving.
   *
   * @param accounts the account repository
   * @param playerGames the player game repository
   */
  void schedulePeriodicSnapshot(AccountRepository accounts, PlayerGameRepository playerGames);

  /** Shuts down any background scheduling. */
  void shutdown();
}
