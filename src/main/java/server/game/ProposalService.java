package server.game;
import shared.dto.GameInfoData;
import server.account.exceptions.InvalidTokenException;
import server.game.exceptions.GameNotFoundException;
import server.game.exceptions.InvalidProposalException;
import java.util.List;

public interface ProposalService {
    GameInfoData submitProposal(String accountToken, List<String> words)
            throws InvalidTokenException, InvalidProposalException;
    GameInfoData getGameInfo(String accountToken, Long gameId)
            throws InvalidTokenException, GameNotFoundException;
    GameInfoData getGameInfoForUsername(long gameId, String username)
            throws GameNotFoundException;
}