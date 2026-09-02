package server.game;

import shared.dto.GameInfoData;
import shared.dto.PlayerGameStatus;
import server.account.exceptions.InvalidTokenException;
import server.game.exceptions.GameNotFoundException;
import server.game.exceptions.InvalidProposalException;

import java.util.List;

/**
 * Service handling proposal submissions and per-player game state retrieval.
 */
public interface ProposalService {

    /**
     * Submits a grouping proposal consisting of exactly 4 words for the current active game.
     * Checks if the words form a correct, unclaimed group and updates the player's game state.
     *
     * @param accountToken the authenticated user's token (JWT)
     * @param words the list of 4 words forming the proposal
     * @return the resulting status of the player's game (ACTIVE, WON, LOST, INCOMPLETE)
     * @throws InvalidTokenException if the account token is missing, invalid, or expired
     * @throws InvalidProposalException if the proposal is malformed, contains unknown words, 
     *         or contains words already grouped (does not count as a game mistake)
     */
    PlayerGameStatus submitProposal(String accountToken, List<String> words)
            throws InvalidTokenException, InvalidProposalException;

    /**
     * Retrieves the current game state and information for the requesting player.
     * For ongoing games, it returns the remaining words and guesses without revealing the solution.
     *
     * @param accountToken the authenticated user's token (JWT)
     * @param gameId the specific game ID to query, or null to target the current active game
     * @return the GameInfoData containing the relevant game state for the client
     * @throws InvalidTokenException if the account token is invalid or expired
     * @throws GameNotFoundException if a specific gameId is provided but does not exist
     */
    GameInfoData getGameInfo(String accountToken, Long gameId)
            throws InvalidTokenException, GameNotFoundException;
}