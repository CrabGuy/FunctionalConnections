package client.formatting;

import java.util.ArrayList;
import java.util.List;
import shared.dto.*;

/** Utility class for formatting various DTOs into human-readable strings for CLI display. */
public final class OutputFormatter {

  private OutputFormatter() {
    // Prevent instantiation
  }

  /**
   * Formats game information for general display, including status, score, and remaining words.
   *
   * @param data the game information
   * @param nowMillis the current time in milliseconds
   * @return a formatted string
   */
  public static String formatGameInfo(GameInfoData data, long nowMillis) {
    List<String> lines = new ArrayList<>();
    lines.add("Game " + data.gameId());
    lines.add("Status: " + GameInfoCalculator.status(data, nowMillis));
    lines.add(
        "Time remaining: "
            + formatDuration(GameInfoCalculator.remainingTimeMillis(data, nowMillis)));
    lines.add("Score: " + GameInfoCalculator.score(data));
    lines.add("Correct proposals: " + GameInfoCalculator.correctProposalCount(data));
    lines.add("Mistakes: " + GameInfoCalculator.mistakeCount(data));
    lines.add("Remaining words: " + GameInfoCalculator.remainingWords(data));
    if (!data.correctGuesses().isEmpty()) {
      lines.add("Correct guesses: " + data.correctGuesses());
    }
    if (!data.wrongGuesses().isEmpty()) {
      lines.add("Wrong guesses: " + data.wrongGuesses());
    }
    data.correctGroupsOptional().ifPresent(groups -> lines.add("Correct groups: " + groups));
    return String.join("\n", lines).trim();
  }

  /**
   * Formats game information specifically for gameplay, with numbered remaining words.
   *
   * @param data the game information
   * @param nowMillis the current time in milliseconds
   * @return a formatted string
   */
  public static String formatGameInfoForPlay(GameInfoData data, long nowMillis) {
    List<String> lines = new ArrayList<>();
    lines.add("Game " + data.gameId());
    lines.add("Status: " + GameInfoCalculator.status(data, nowMillis));
    lines.add(
        "Time remaining: "
            + formatDuration(GameInfoCalculator.remainingTimeMillis(data, nowMillis)));
    lines.add("Score: " + GameInfoCalculator.score(data));
    lines.add("Correct proposals: " + GameInfoCalculator.correctProposalCount(data));
    lines.add("Mistakes: " + GameInfoCalculator.mistakeCount(data));

    List<String> remaining = GameInfoCalculator.remainingWords(data);
    lines.add("Remaining words:");
    for (int i = 0; i < remaining.size(); i++) {
      lines.add(String.format("  %d. %s", i + 1, remaining.get(i)));
    }

    if (!data.correctGuesses().isEmpty()) {
      lines.add("Correct guesses: " + data.correctGuesses());
    }
    if (!data.wrongGuesses().isEmpty()) {
      lines.add("Wrong guesses: " + data.wrongGuesses());
    }
    data.correctGroupsOptional().ifPresent(groups -> lines.add("Correct groups: " + groups));
    return String.join("\n", lines).trim();
  }

  /**
   * Formats game statistics for display.
   *
   * @param data the game statistics
   * @param nowMillis the current time in milliseconds
   * @return a formatted string
   */
  public static String formatGameStats(GameStatsData data, long nowMillis) {
    List<String> lines = new ArrayList<>();
    lines.add("Game statistics for " + data.gameId());
    lines.add("Completed: " + data.completed());
    lines.add("Time remaining: " + formatDuration(Math.max(0L, data.expiresAt() - nowMillis)));
    lines.add("Total participants: " + data.totalParticipants());
    lines.add("Active players: " + data.activePlayers());
    lines.add("Completed players: " + data.completedPlayers());
    lines.add("Winners: " + data.winners());
    lines.add("Average score: " + data.averageScore());
    return String.join("\n", lines).trim();
  }

  /**
   * Formats leaderboard data for display.
   *
   * @param data the leaderboard data
   * @return a formatted string
   */
  public static String formatLeaderboard(LeaderboardData data) {
    List<String> lines = new ArrayList<>();
    lines.add("Leaderboard (" + data.totalPlayers() + " players)");
    for (LeaderboardEntry entry : data.topPlayers()) {
      lines.add(String.format("%d. %s — %d", entry.rank(), entry.username(), entry.score()));
    }
    if (data.requestedPlayer() != null) {
      LeaderboardEntry entry = data.requestedPlayer();
      lines.add(
          "Requested player: "
              + entry.username()
              + " — rank "
              + entry.rank()
              + ", score "
              + entry.score());
    }
    return String.join("\n", lines).trim();
  }

  /**
   * Formats player statistics for display.
   *
   * @param data the player statistics
   * @return a formatted string
   */
  public static String formatPlayerStats(PlayerStatsData data) {
    List<String> lines = new ArrayList<>();
    lines.add("Puzzles completed: " + data.puzzlesCompleted());
    lines.add("Win rate: " + formatPercent(data.winRate()));
    lines.add("Loss rate: " + formatPercent(data.lossRate()));
    lines.add("Current streak: " + data.currentStreak());
    lines.add("Max streak: " + data.maxStreak());
    lines.add("Perfect puzzles: " + data.perfectPuzzles());
    lines.add("Mistake histogram: " + data.mistakeHistogram());
    return String.join("\n", lines).trim();
  }

  /**
   * Formats an error message for display.
   *
   * @param message the error message
   * @return a formatted error string
   */
  public static String formatError(String message) {
    return "Request failed: " + message;
  }

  /**
   * Formats a duration in milliseconds as HH:MM:SS or MM:SS.
   *
   * @param millis the duration in milliseconds
   * @return a formatted time string
   */
  private static String formatDuration(long millis) {
    long totalSeconds = millis / 1000;
    long hours = totalSeconds / 3600;
    long minutes = (totalSeconds % 3600) / 60;
    long seconds = totalSeconds % 60;
    return hours > 0
        ? String.format("%02d:%02d:%02d", hours, minutes, seconds)
        : String.format("%02d:%02d", minutes, seconds);
  }

  /**
   * Formats a decimal fraction as a percentage with two decimal places.
   *
   * @param value the fraction (e.g., 0.75)
   * @return a percentage string (e.g., "75.00%")
   */
  private static String formatPercent(double value) {
    return String.format("%.2f%%", value * 100.0);
  }
}
