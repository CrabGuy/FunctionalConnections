package server.game;

import server.dto.PlayerGame;
import java.util.List;

/**
 * Repository interface for managing player game states and their submitted proposals.
 */
public interface PlayerGameRepository {

    /**
     * Retrieves an existing game state for a player, or creates a new empty one if they 
     * are participating in this game for the first time.
     *
     * @param username the username of the player
     * @param gameId the ID of the game
     * @return the existing or newly created PlayerGame record
     */
    PlayerGame findOrCreate(String username, long gameId);

    /**
     * Persists a player's game state after a new proposal or status change.
     * Data is kept in memory and queued for saving.
     *
     * @param playerGame the player game state to save
     */
    void save(PlayerGame playerGame);

    /**
     * Retrieves all player game states associated with a specific game ID.
     * Used primarily by statistics and leaderboard calculations.
     *
     * @param gameId the game ID
     * @return a list of PlayerGame states for the given game
     */
    List<PlayerGame> findByGame(long gameId);

    /**
     * Retrieves all game states associated with a specific username across all games.
     *
     * @param username the username
     * @return a list of PlayerGame states for the given user
     */
    List<PlayerGame> findPlayerGameByUsername(String username);
}
