package server.game;

import shared.dto.GameInfoData;
import server.account.exceptions.InvalidTokenException;
import server.game.exceptions.GameNotCurrentException;
import server.game.exceptions.GameNotFoundException;
import server.game.exceptions.InvalidProposalException;
import server.game.exceptions.PlayerAlreadyCompletedGameException;

import java.util.List;

public interface ProposalService {
    GameInfoData submitProposal(String accountToken, long gameId, List<String> words)
            throws InvalidTokenException, InvalidProposalException,
                   GameNotCurrentException, PlayerAlreadyCompletedGameException;

    GameInfoData getGameInfo(String accountToken, Long gameId)
            throws InvalidTokenException, GameNotFoundException;

    GameInfoData getGameInfoForUsername(long gameId, String username)
            throws GameNotFoundException;
}