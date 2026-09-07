package server.dto;

import java.util.List;

/**
 * Represents the correct word groups for a specific game.
 *
 * @param gameId the game ID
 * @param groups the list of correct word groups
 */
public record GameWordGroups(long gameId, List<WordGroup> groups) {

  public GameWordGroups {
    groups = List.copyOf(groups);
  }
}
