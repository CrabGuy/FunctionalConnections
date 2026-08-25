package client;

import shared.DataContracts;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.IntStream;

public final class UiHandler {
    public record Credentials(String username, String password) {}

    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static String promptText(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public static String showAuthMenu(Scanner scanner) {
        clearScreen();
        System.out.println("==========================================");
        System.out.println("          === CONNECTIONS GAME ===");
        System.out.println("==========================================");
        System.out.println("1: Register\n2: Login\n3: Update Credentials\n4: Exit");
        return promptText(scanner, "Choose action: ");
    }

    public static String showGameMenu(Scanner scanner, String username, DataContracts.GameStateDto board) {
        clearScreen();
        System.out.println("==========================================");
        System.out.println("      --- MAIN MENU (" + username + ") ---");
        System.out.println("==========================================");
        if (board != null) renderGameBoard(board);
        System.out.println("\n------------------------------------------");
        System.out.println("1: Play Active Game\n2: Game Statistics\n3: Player Overall Stats");
        System.out.println("4: Leaderboards\n5: Update Credentials\n6: Logout");
        return promptText(scanner, "Choose option: ");
    }

    public static Optional<Credentials> readCredentials(Scanner scanner, String action) {
        clearScreen();
        System.out.println("--- " + action + " ---");
        String username = promptText(scanner, "Username: ");
        String password = promptText(scanner, "Password: ");
        return Optional.of(new Credentials(username, password))
                .filter(c -> !c.username().isBlank() && !c.password().isBlank());
    }

    public static String readTargetPlayer(Scanner scanner) {
        return promptText(scanner, "Search specific player (press Enter for Top 10): ");
    }

    public static String promptProposal(Scanner scanner) {
        return promptText(scanner, "\nEnter 4 word numbers or words (or 'back'): ");
    }

    public static List<String> parseProposalInput(String input, List<String> availableWords) {
        List<String> tokens = Arrays.stream(input.split("[,\\s]+")).filter(t -> !t.isBlank()).map(String::trim).toList();
        if (tokens.stream().allMatch(t -> t.matches("\\d+"))) {
            return tokens.stream().map(Integer::parseInt)
                    .filter(i -> i >= 1 && i <= availableWords.size())
                    .map(i -> availableWords.get(i - 1))
                    .distinct().toList();
        }
        return tokens.stream().distinct().toList();
    }

    public static void renderGameBoard(DataContracts.GameStateDto board) {
        System.out.println("\n           CONNECTIONS BOARD");
        System.out.println("==========================================");
        
        if (board.solvedGroups() != null && !board.solvedGroups().isEmpty()) {
            System.out.println("Solved Groups:");
            IntStream.range(0, board.solvedGroups().size())
                    .forEach(i -> System.out.println("  Group " + (i + 1) + ": " + String.join(", ", board.solvedGroups().get(i))));
            System.out.println("------------------------------------------");
        }
        
        if (board.remainingWords() != null && !board.remainingWords().isEmpty()) {
            System.out.println("Remaining Words:");
            IntStream.range(0, board.remainingWords().size())
                    .forEach(i -> System.out.printf("%2d) %-15s%s", (i + 1), board.remainingWords().get(i), (i + 1) % 4 == 0 ? "\n" : ""));
            if (board.remainingWords().size() % 4 != 0) System.out.println();
            System.out.println("------------------------------------------");
        }

        if (board.allGroups() != null && !board.allGroups().isEmpty()) {
            System.out.println("Correct Groups:");
            board.allGroups().forEach(g -> System.out.println("  " + g));
        }

        System.out.print("Mistakes made: " + board.mistakes());
    }

    public static void printLeaderboard(DataContracts.LeaderboardDto result) {
        if (result == null) {
            System.out.println("[!] Leaderboard unavailable.");
            return;
        }
        System.out.println("\n--- LEADERBOARD ---");
        if (result.position() != null) {
            System.out.println("Player Rank: " + result.position());
        } else if (result.entries() != null) {
            result.entries().forEach(e -> System.out.printf("• %-15s - %d wins\n", e.username(), e.wins()));
        }
    }

    public static void printPlayerStats(DataContracts.PlayerStatsDto result) {
        if (result == null) {
            System.out.println("[!] Stats unavailable.");
            return;
        }
        System.out.println("\n--- PLAYER STATS ---");
        System.out.printf("%-20s: %d\n", "Puzzles Completed", result.puzzlesCompleted());
        System.out.printf("%-20s: %.1f%%\n", "Win Rate", result.winRate());
        System.out.printf("%-20s: %.1f%%\n", "Loss Rate", result.lossRate());
        System.out.printf("%-20s: %d\n", "Current Streak", result.currentStreak());
        System.out.printf("%-20s: %d\n", "Max Streak", result.maxStreak());
        System.out.printf("%-20s: %d\n", "Perfect Puzzles", result.perfectPuzzles());
        
        System.out.println("Mistake Histogram:");
        if ("NONE".equals(result.mistakeHistogram())) {
            System.out.println("  No completed games yet.");
        } else {
            Arrays.stream(result.mistakeHistogram().split(",")).forEach(entry -> {
                String[] parts = entry.split(":");
                if (parts.length >= 2) System.out.printf("  %s mistakes: %s game(s)\n", parts[0], parts[1]);
            });
        }
    }

    public static void printGameStats(DataContracts.GameStatsDto result) {
        if (result == null) {
            System.out.println("[!] Game stats unavailable.");
            return;
        }
        System.out.println("\n--- GAME STATISTICS ---");
        System.out.printf("%-20s: %d\n", "Game ID", result.gameId());
        System.out.printf("%-20s: %d\n", "Remaining Time(ms)", result.remainingTimeMs());
        System.out.printf("%-20s: %d\n", "Total Players", result.totalPlayers());
        System.out.printf("%-20s: %d\n", "In Progress", result.inProgressPlayers());
        System.out.printf("%-20s: %d\n", "Finished Players", result.finishedPlayers());
        System.out.printf("%-20s: %d\n", "Wins", result.wins());
        if (result.remainingTimeMs() == 0) {
            System.out.printf("%-20s: %.1f\n", "Avg Score", result.avgScore());
        }
    }

    public static void pauseForUser(Scanner scanner) {
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }
}