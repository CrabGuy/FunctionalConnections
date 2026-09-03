package shared.dto;
import java.util.List;
public record SubmitProposalRequest(String accountToken, Long gameId, List<String> words) implements ApiRequest {
    public SubmitProposalRequest {
        words = List.copyOf(words);
    }

    public SubmitProposalRequest(String accountToken, List<String> words) {
        this(accountToken, null, words);
    }

    @Override
    public String getOperation() {
        return "submitProposal";
    }
}