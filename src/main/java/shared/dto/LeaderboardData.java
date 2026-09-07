package shared.dto;

import java.util.List;

/**
 * Leaderboard data including top players, optionally a requested player, and total players.
 *
 * @param topPlayers list of top entries (may be limited)
 * @param requestedPlayer the requested player's entry, or {@code null}
 * @param totalPlayers total number of players in the leaderboard
 */
public record LeaderboardData(
    List<LeaderboardEntry> topPlayers, LeaderboardEntry requestedPlayer, int totalPlayers) {

  public LeaderboardData {
    topPlayers = List.copyOf(topPlayers);
  }
}
