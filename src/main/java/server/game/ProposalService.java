package server.game;

import java.util.List;
import server.account.exceptions.InvalidTokenException;
import server.game.exceptions.GameNotCurrentException;
import server.game.exceptions.GameNotFoundException;
import server.game.exceptions.InvalidProposalException;
import server.game.exceptions.PlayerAlreadyCompletedGameException;
import shared.dto.GameInfoData;

/** Service for handling game proposals and retrieving game information. */
public interface ProposalService {

  /**
   * Submits a proposal for the current game.
   *
   * @param accountToken the account token
   * @param gameId the game ID (must be the current game)
   * @param words the proposed words
   * @return the updated game information
   * @throws InvalidTokenException if the token is invalid
   * @throws InvalidProposalException if the proposal is malformed or invalid
   * @throws GameNotCurrentException if the game is not current
   * @throws PlayerAlreadyCompletedGameException if the player already completed the game
   */
  GameInfoData submitProposal(String accountToken, long gameId, List<String> words)
      throws InvalidTokenException,
          InvalidProposalException,
          GameNotCurrentException,
          PlayerAlreadyCompletedGameException;

  /**
   * Retrieves game information for a specified or current game.
   *
   * @param accountToken the account token
   * @param gameId the game ID, or {@code null} for current game
   * @return the game information
   * @throws InvalidTokenException if the token is invalid
   * @throws GameNotFoundException if the game does not exist
   */
  GameInfoData getGameInfo(String accountToken, Long gameId)
      throws InvalidTokenException, GameNotFoundException;

  /**
   * Ensures that the player has a record for the current game.
   *
   * @param accountToken the account token
   * @throws InvalidTokenException if the token is invalid
   */
  void touchCurrentGame(String accountToken) throws InvalidTokenException;
}
