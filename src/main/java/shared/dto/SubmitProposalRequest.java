package shared.dto;

import java.util.List;

/**
 * Request to submit a proposal of four words that the player believes form a group.
 * Operation: "submitProposal"
 *
 * @param accountToken the JWT token identifying the logged-in user.
 * @param words        the four words proposed.
 */
public record SubmitProposalRequest(String accountToken, List<String> words) implements ApiRequest {
    public SubmitProposalRequest {
        words = List.copyOf(words);
    }

    @Override
    public String getOperation() {
        return "submitProposal";
    }
}
