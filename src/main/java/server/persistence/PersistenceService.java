package server.persistence;

import server.account.AccountRepository;
import server.game.PlayerGameRepository;

/**
 * Handles durable storage of accounts and player games.
 * <p>
 * It receives the current in‑memory repositories as parameters and
 * persists their contents atomically. On startup, it can populate the
 * repositories from a previously saved snapshot.
 * </p>
 */
public interface PersistenceService {

    /**
     * Writes the current state of both repositories to disk as a single
     * atomic snapshot.
     *
     * @param accounts   the account repository to persist
     * @param playerGames the player‑game repository to persist
     * @throws java.io.IOException if an I/O error occurs during writing
     */
    void saveSnapshot(AccountRepository accounts, PlayerGameRepository playerGames) throws java.io.IOException;

    /**
     * Loads a previously saved snapshot from disk and populates the given
     * repositories.
     * <p>
     * The repositories should be empty when this method is called.
     * </p>
     *
     * @param accounts   the account repository to populate
     * @param playerGames the player‑game repository to populate
     * @throws java.io.IOException if the snapshot cannot be read or is malformed
     */
    void loadSnapshot(AccountRepository accounts, PlayerGameRepository playerGames) throws java.io.IOException;

    /**
     * Schedules a periodic background task that calls {@link #saveSnapshot}
     * with the current repositories.
     * <p>
     * The repositories must be supplied when scheduling; the service may
     * hold a reference to them, or the caller can provide them on each tick.
     * </p>
     *
     * @param accounts   the account repository
     * @param playerGames the player‑game repository
     */
    void schedulePeriodicSnapshot(AccountRepository accounts, PlayerGameRepository playerGames);
}