package server.dto;

import java.util.Set;

public record Proposal(Set<String> words) {
  public Proposal {
    words = Set.copyOf(words);
  }
}
