package shared.dto;
import java.util.List;
public record SubmitProposalRequest(String accountToken, List<String> words) implements ApiRequest {
    public SubmitProposalRequest {
        words = List.copyOf(words);
    }
    @Override
    public String getOperation() {
        return "submitProposal";
    }
}
