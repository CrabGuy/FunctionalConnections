package server.dto;

import java.util.List;

public record PlayerGame(String username, long gameId, List<Proposal> proposals) {
  public PlayerGame {
    proposals = List.copyOf(proposals);
  }
}
