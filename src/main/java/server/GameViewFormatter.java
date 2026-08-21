package server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public final class GameViewFormatter {
    private final GameManager gameManager;
    private final GameQueryService query;

    public GameViewFormatter(GameManager gameManager, GameQueryService query) {
        this.gameManager = gameManager;
        this.query = query;
    }

    public String buildGameState(GameManager.Game game, String player, GameManager.Status status) {
        GameManager.PlayerProgress playerProgress = query.progress(game, player);
        StringBuilder builder = new StringBuilder();

        builder.append("GAME_ID:").append(game.id()).append("\n");
        builder.append("STATUS:").append(status).append("\n");

        if (status == GameManager.Status.IN_PROGRESS) {
            builder.append("REMAINING_TIME_MS:").append(gameManager.getRemainingTime(game).toMillis()).append("\n");
            builder.append("SCORE:").append(playerProgress.solvedCount()).append("\n");
            builder.append("MISTAKES:").append(playerProgress.mistakesMade()).append("\n");
            builder.append("SOLVED_GROUPS:").append(formatSolvedGroups(playerProgress)).append("\n");
            builder.append("REMAINING_WORDS:").append(formatRemainingWords(game, player));
        } else {
            builder.append("SCORE:").append(playerProgress.solvedCount()).append("\n");
            builder.append("CORRECT_PROPOSALS:").append(playerProgress.solvedCount()).append("\n");
            builder.append("MISTAKES:").append(playerProgress.mistakesMade()).append("\n");
            builder.append("GROUPS:").append(formatCorrectGroups(game));
        }

        return builder.toString();
    }

    public String buildGameStatistics(GameManager.Game game) {
        List<String> players = new ArrayList<>(game.playerStates().keySet());
        long total = players.size();
        long finished = players.stream()
                .filter(player -> gameManager.getPlayerStatus(game, player) != GameManager.Status.IN_PROGRESS)
                .count();
        long wins = players.stream()
                .filter(player -> gameManager.getPlayerStatus(game, player) == GameManager.Status.WON)
                .count();

        String base = "GAME_ID:" + game.id() + "\n";

        if (gameManager.getRemainingTime(game).isZero()) {
            double averageScore = total == 0
                    ? 0
                    : players.stream()
                            .mapToInt(player -> (int) query.progress(game, player).solvedCount())
                            .average()
                            .orElse(0);
            return base +
                    "TOTAL_PLAYERS:" + total +
                    "\nFINISHED:" + finished +
                    "\nWINS:" + wins +
                    "\nAVG_SCORE:" + averageScore;
        }

        long inProgress = total - finished;
        return base +
                "REMAINING_TIME_MS:" + gameManager.getRemainingTime(game).toMillis() +
                "\nIN_PROGRESS_PLAYERS:" + inProgress +
                "\nFINISHED:" + finished +
                "\nWINS:" + wins;
    }

    public String buildPersonalStats(User user) {
        int completed = user.games.size();
        int wins = user.getWins();
        int losses = completed - wins;
        double winRate = completed == 0 ? 0 : wins * 100.0 / completed;
        double lossRate = completed == 0 ? 0 : losses * 100.0 / completed;

        String histogram = user.getMistakeHistogram().entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(","));

        return "PUZZLES_COMPLETED:" + completed +
                "\nWIN_RATE:" + String.format("%.1f", winRate) +
                "\nLOSS_RATE:" + String.format("%.1f", lossRate) +
                "\nCURRENT_STREAK:" + user.currentStreak +
                "\nMAX_STREAK:" + user.maxStreak +
                "\nPERFECT_PUZZLES:" + user.getPerfectPuzzles() +
                "\nMISTAKE_HISTOGRAM:" + (histogram.isEmpty() ? "NONE" : histogram);
    }

    private String formatSolvedGroups(GameManager.PlayerProgress playerProgress) {
        String solved = playerProgress.history().stream()
                .filter(GameManager.Guess::isCorrect)
                .map(guess -> String.join(",", guess.words()))
                .collect(Collectors.joining(";"));
        return solved.isEmpty() ? "NONE" : solved;
    }

    private String formatRemainingWords(GameManager.Game game, String player) {
        Set<String> solved = query.solvedWords(game, player);
        List<String> remaining = game.wordGroups().stream()
                .flatMap(group -> group.words().stream())
                .distinct()
                .filter(word -> !solved.contains(word.toUpperCase()))
                .collect(Collectors.toList());
        Collections.shuffle(remaining, new Random(game.id()));
        return String.join(", ", remaining);
    }

    private String formatCorrectGroups(GameManager.Game game) {
        return game.wordGroups().stream()
                .map(group -> group.category() + ":" + String.join(",", group.words()))
                .collect(Collectors.joining(" | "));
    }
}