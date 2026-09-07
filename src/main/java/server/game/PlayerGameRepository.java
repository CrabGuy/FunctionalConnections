package server.game;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import server.dto.PlayerGame;

/** Repository for storing and retrieving player game data. */
public interface PlayerGameRepository {

  /**
   * Finds the player's game record or creates a new empty one if it doesn't exist.
   *
   * @param username the username
   * @param gameId the game ID
   * @return the player game record
   */
  PlayerGame findOrCreate(String username, long gameId);

  /**
   * Saves or updates a player game record.
   *
   * @param playerGame the player game to save
   */
  void save(PlayerGame playerGame);

  /**
   * Returns all player game records for a given game.
   *
   * @param gameId the game ID
   * @return a list of player game records
   */
  List<PlayerGame> findByGame(long gameId);

  /**
   * Returns all player game records for a given username.
   *
   * @param username the username
   * @return a list of player game records
   */
  List<PlayerGame> findPlayerGameByUsername(String username);

  /**
   * Returns the set of all usernames that have at least one player game record.
   *
   * @return a set of usernames
   */
  Set<String> findAllUsernames();

  /**
   * Finds a player game record by username and game ID.
   *
   * @param username the username
   * @param gameId the game ID
   * @return an Optional containing the record if found
   */
  Optional<PlayerGame> findByUsernameAndGame(String username, long gameId);

  /**
   * Returns all player game records.
   *
   * @return a list of all player game records
   */
  List<PlayerGame> findAll();

  /**
   * Updates the username in all player game records for a user.
   *
   * @param oldUsername the old username
   * @param newUsername the new username
   */
  void updateUsername(String oldUsername, String newUsername);
}
