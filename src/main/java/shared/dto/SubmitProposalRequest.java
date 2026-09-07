package shared.dto;

import java.util.List;

/**
 * Request to submit a proposal for the current or specified game.
 *
 * @param operation the operation name ("submitProposal")
 * @param accountToken the account token
 * @param gameId optional game ID, or {@code null} for current game
 * @param words the four proposed words
 */
public record SubmitProposalRequest(
    String operation, String accountToken, Long gameId, List<String> words) implements ApiRequest {

  /**
   * Convenience constructor for current game.
   *
   * @param accountToken the account token
   * @param words the proposed words
   */
  public SubmitProposalRequest(String accountToken, List<String> words) {
    this("submitProposal", accountToken, null, words);
  }

  /**
   * Constructor with explicit game ID.
   *
   * @param accountToken the account token
   * @param gameId the game ID
   * @param words the proposed words
   */
  public SubmitProposalRequest(String accountToken, Long gameId, List<String> words) {
    this("submitProposal", accountToken, gameId, words);
  }
}
