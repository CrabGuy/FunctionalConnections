package server.dto;

import java.util.List;

/**
 * Represents a player's proposals for a specific game.
 *
 * @param username the player's username
 * @param gameId the game ID
 * @param proposals the list of proposals made by the player
 */
public record PlayerGame(String username, long gameId, List<Proposal> proposals) {

  public PlayerGame {
    proposals = List.copyOf(proposals);
  }
}
