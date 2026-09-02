package server.game;

import server.dto.GameWordGroups;
import server.game.exceptions.GameNotFoundException;

/**
 * Read-only access to the game catalog (the 16-word / 4-group puzzles
 * sourced from the on-disk JSON data file).
 *
 * <p>This is a boundary interface: implementations perform file I/O and
 * must not load the entire catalog into memory at once. Per the notes,
 * entries should be read lazily (e.g. via a streaming/lazy JSON iterator)
 * and only the requested {@link GameWordGroups} materialized.
 *
 * <p>The catalog is finite (currently 911 games) while {@link GameClock}
 * produces an ever-increasing sequence of game identifiers, so
 * implementations are expected to loop back to the start of the catalog
 * once it is exhausted (e.g. {@code catalogIndex = gameId % catalogSize}).
 *
 * <p>Only mock this interface at the boundary in tests; it must not leak
 * file-format or streaming details into callers.
 */
public interface GameRepository {

    /**
     * Loads the word groups for the given game, wrapping around the
     * catalog if {@code gameId} exceeds the number of available games.
     *
     * @param gameId the game identifier to load
     * @return the word groups for that game
     * @throws GameNotFoundException if the catalog is empty or otherwise
     *                                cannot resolve {@code gameId}
     */
    GameWordGroups loadById(long gameId) throws GameNotFoundException;

    /**
     * Checks whether the given game identifier can be resolved to a
     * catalog entry, without materializing its word groups.
     *
     * @param gameId the game identifier to check
     * @return {@code true} if {@link #loadById(long)} would succeed for
     *         this identifier, {@code false} otherwise
     */
    boolean exists(long gameId);
}