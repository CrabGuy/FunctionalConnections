package server.dto;

import java.util.List;

/**
 * Represents the full solution for one game: all 4 word groups.
 * This is loaded from the JSON data file and is immutable.
 *
 * @param gameId the unique identifier of the game.
 * @param groups the four groups that form the solution.
 */
public record GameWordGroups(long gameId, List<WordGroup> groups) {
    public GameWordGroups {
        groups = List.copyOf(groups);
    }
}
