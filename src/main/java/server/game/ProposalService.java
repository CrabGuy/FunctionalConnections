package server.game;

import java.util.List;
import server.account.exceptions.InvalidTokenException;
import server.game.exceptions.GameNotCurrentException;
import server.game.exceptions.GameNotFoundException;
import server.game.exceptions.InvalidProposalException;
import server.game.exceptions.PlayerAlreadyCompletedGameException;
import shared.dto.GameInfoData;

public interface ProposalService {
  GameInfoData submitProposal(String accountToken, long gameId, List<String> words)
      throws InvalidTokenException,
          InvalidProposalException,
          GameNotCurrentException,
          PlayerAlreadyCompletedGameException;

  GameInfoData getGameInfo(String accountToken, Long gameId)
      throws InvalidTokenException, GameNotFoundException;

  GameInfoData getGameInfoForUsername(long gameId, String username) throws GameNotFoundException;
}
