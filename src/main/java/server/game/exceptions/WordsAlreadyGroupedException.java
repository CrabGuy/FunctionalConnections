package server.game.exceptions;
import shared.dto.ErrorCode;
public final class WordsAlreadyGroupedException extends InvalidProposalException {
    public WordsAlreadyGroupedException() {
        super(ErrorCode.WORDS_ALREADY_GROUPED, "One or more words in the proposal have already been grouped.");
    }
}
