package server.game;

/**
 * Derives game identity and lifecycle timestamps purely from wall-clock time.
 *
 * <p>There is exactly one global game active at any given moment, and games
 * are never persisted as standalone metadata: a game's identity, start time
 * and expiry are all deterministic functions of the current time and the
 * game duration. This keeps the server from having to
 * track "which game is running" as mutable state — any component can ask
 * "what game is it right now?" and get a consistent answer.
 *
 * <p>All methods are pure and side-effect free: given the same arguments
 * they always return the same result. Callers are responsible for supplying
 * {@code nowMillis} (typically {@code System.currentTimeMillis()}) so that
 * this component never reads system time itself, per the functional-core /
 * imperative-shell split.
 *
 * <p>Implementations are expected to be constructed with the configured
 * game duration.
 */
public interface GameClock {

    /**
     * Computes the identifier of the game that is active at the given
     * instant. Game identifiers are contiguous and increase monotonically
     * with time; there is no gap between one game's expiry and the next
     * game's start.
     *
     * @param nowMillis the instant to resolve, as epoch milliseconds
     * @return the identifier of the game active at {@code nowMillis}
     */
    long currentGameId(long nowMillis);

    /**
     * Computes the instant at which the given game became (or will become)
     * active.
     *
     * @param gameId the game identifier
     * @return the game's start instant, as epoch milliseconds
     */
    long startedAt(long gameId);

    /**
     * Computes the instant at which the given game expires (or expired).
     *
     * @param gameId the game identifier
     * @return the game's expiry instant, as epoch milliseconds
     */
    long expiresAt(long gameId);

    /**
     * Determines whether the given game has already expired as of the
     * given instant.
     *
     * @param gameId    the game identifier
     * @param nowMillis the instant to evaluate against, as epoch milliseconds
     * @return {@code true} if {@code nowMillis} is at or after the game's
     *         expiry instant, {@code false} otherwise
     */
    boolean isCompleted(long gameId, long nowMillis);
}