package server.dto;

import java.util.Set;

/**
 * Represents a proposal of exactly four distinct words.
 *
 * @param words the set of words (must be exactly four)
 */
public record Proposal(Set<String> words) {

  public Proposal {
    words = Set.copyOf(words);
  }
}
