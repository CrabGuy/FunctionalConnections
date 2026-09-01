package server.dto;

import java.util.List;

/**
 * Stores a player's activity in a specific game.
 * All proposals (correct and wrong) are recorded in order.
 * This is the persistent data from which all game state and statistics are derived.
 *
 * @param username  the player's username.
 * @param gameId    the ID of the game.
 * @param proposals the list of proposals made by this player in this game.
 */
public record PlayerGame(String username, long gameId, List<Proposal> proposals) {
    public PlayerGame {
        proposals = List.copyOf(proposals);
    }
}
