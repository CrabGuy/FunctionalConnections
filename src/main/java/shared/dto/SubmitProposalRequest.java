package shared.dto;

import java.util.List;

/**
 * Request to submit a proposal of four words that the player believes form a group.
 * Operation: "submitProposal"
 */
public record SubmitProposalRequest(List<String> words) implements ApiRequest {
    public SubmitProposalRequest {
        // Defensive copy to ensure immutability
        words = List.copyOf(words);
    }

    @Override
    public String getOperation() {
        return "submitProposal";
    }
}
