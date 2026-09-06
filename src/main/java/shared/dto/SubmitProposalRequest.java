package shared.dto;

import java.util.List;

public record SubmitProposalRequest(
    String operation, String accountToken, Long gameId, List<String> words) implements ApiRequest {
  public SubmitProposalRequest(String accountToken, List<String> words) {
    this("submitProposal", accountToken, null, words);
  }

  public SubmitProposalRequest(String accountToken, Long gameId, List<String> words) {
    this("submitProposal", accountToken, gameId, words);
  }
}
