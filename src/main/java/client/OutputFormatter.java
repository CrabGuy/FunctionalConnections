package client;

import shared.dto.GameInfoData;
import shared.dto.GameStatsData;
import shared.dto.LeaderboardData;
import shared.dto.LeaderboardEntry;
import shared.dto.PlayerStatsData;

public final class OutputFormatter {
    private OutputFormatter() {}

    public static String formatGameInfo(GameInfoData data, long nowMillis) {
        StringBuilder sb = new StringBuilder();
        sb.append("Game ").append(data.gameId()).append('\n');
        sb.append("Status: ").append(GameInfoCalculator.status(data, nowMillis)).append('\n');
        sb.append("Time remaining: ").append(formatDuration(GameInfoCalculator.remainingTimeMillis(data, nowMillis))).append('\n');
        sb.append("Score: ").append(GameInfoCalculator.score(data)).append('\n');
        sb.append("Correct proposals: ").append(GameInfoCalculator.correctProposalCount(data)).append('\n');
        sb.append("Mistakes: ").append(GameInfoCalculator.mistakeCount(data)).append('\n');
        sb.append("Remaining words: ").append(GameInfoCalculator.remainingWords(data)).append('\n');
        if (!data.correctGuesses().isEmpty()) {
            sb.append("Correct guesses: ").append(data.correctGuesses()).append('\n');
        }
        if (!data.wrongGuesses().isEmpty()) {
            sb.append("Wrong guesses: ").append(data.wrongGuesses()).append('\n');
        }
        if (data.correctGroups() != null) {
            sb.append("Correct groups: ").append(data.correctGroups()).append('\n');
        }
        return sb.toString().trim();
    }

    public static String formatGameStats(GameStatsData data, long nowMillis) {
        StringBuilder sb = new StringBuilder();
        sb.append("Game statistics for ").append(data.gameId()).append('\n');
        sb.append("Completed: ").append(data.completed()).append('\n');
        sb.append("Time remaining: ").append(formatDuration(
                Math.max(0L, data.expiresAt() - nowMillis))).append('\n');
        sb.append("Total participants: ").append(data.totalParticipants()).append('\n');
        sb.append("Active players: ").append(data.activePlayers()).append('\n');
        sb.append("Completed players: ").append(data.completedPlayers()).append('\n');
        sb.append("Winners: ").append(data.winners()).append('\n');
        sb.append("Average score: ").append(data.averageScore()).append('\n');
        return sb.toString().trim();
    }

    public static String formatLeaderboard(LeaderboardData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("Leaderboard (").append(data.totalPlayers()).append(" players)").append('\n');
        for (LeaderboardEntry entry : data.topPlayers()) {
            sb.append(String.format("%d. %s — %d%n", entry.rank(), entry.username(), entry.score()));
        }
        if (data.requestedPlayer() != null) {
            LeaderboardEntry entry = data.requestedPlayer();
            sb.append("Requested player: ").append(entry.username())
              .append(" — rank ").append(entry.rank())
              .append(", score ").append(entry.score()).append('\n');
        }
        return sb.toString().trim();
    }

    public static String formatPlayerStats(PlayerStatsData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("Puzzles completed: ").append(data.puzzlesCompleted()).append('\n');
        sb.append("Win rate: ").append(formatPercent(data.winRate())).append('\n');
        sb.append("Loss rate: ").append(formatPercent(data.lossRate())).append('\n');
        sb.append("Current streak: ").append(data.currentStreak()).append('\n');
        sb.append("Max streak: ").append(data.maxStreak()).append('\n');
        sb.append("Perfect puzzles: ").append(data.perfectPuzzles()).append('\n');
        sb.append("Mistake histogram: ").append(data.mistakeHistogram()).append('\n');
        return sb.toString().trim();
    }

    public static String formatError(String message) {
        return "Request failed: " + message;
    }

    private static String formatDuration(long millis) {
        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return hours > 0
                ? String.format("%02d:%02d:%02d", hours, minutes, seconds)
                : String.format("%02d:%02d", minutes, seconds);
    }

    private static String formatPercent(double value) {
        return String.format("%.2f%%", value * 100.0);
    }
}
