package client;

import shared.Response;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;

public final class UiHandler {

    public record Credentials(String username, String password) {}

    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static String showAuthMenu(Scanner scanner) {
        System.out.println("==========================================");
        System.out.println("          === CONNECTIONS GAME ===");
        System.out.println("==========================================");
        System.out.println("1: Register");
        System.out.println("2: Login");
        System.out.println("3: Update Credentials");
        System.out.println("4: Exit");
        System.out.print("Choose action: ");
        return scanner.nextLine().trim();
    }

    public static String showGameMenu(Scanner scanner, String username) {
        System.out.println("\n==========================================");
        System.out.println("      --- MAIN MENU (" + username + ") ---");
        System.out.println("==========================================");
        System.out.println("1: Play Active Game");
        System.out.println("2: Refresh Game Info");
        System.out.println("3: Current Game Stats");
        System.out.println("4: Player Overall Stats");
        System.out.println("5: Leaderboards");
        System.out.println("6: Update Credentials");
        System.out.println("7: Logout");
        System.out.print("Choose option: ");
        return scanner.nextLine().trim();
    }

    public static Optional<Credentials> readCredentials(Scanner scanner, String action) {
        System.out.println("\n--- " + action + " ---");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();
        return Optional.of(new Credentials(username, password))
                .filter(c -> !c.username().isBlank() && !c.password().isBlank());
    }

    public static String readTargetPlayer(Scanner scanner) {
        System.out.print("Search specific player (press Enter for Top 10): ");
        return scanner.nextLine().trim();
    }

    public static List<String> parseProposalInput(String input, List<String> availableWords) {
        List<String> rawTokens = Arrays.stream(input.split("[,\\s]+"))
                .filter(s -> !s.isBlank())
                .map(String::trim)
                .toList();

        boolean isNumeric = rawTokens.stream().allMatch(t -> t.matches("\\d+"));
        if (isNumeric) {
            return rawTokens.stream()
                    .map(Integer::parseInt)
                    .filter(idx -> idx >= 1 && idx <= availableWords.size())
                    .map(idx -> availableWords.get(idx - 1))
                    .distinct()
                    .toList();
        }

        return rawTokens;
    }

    public static String promptProposal(Scanner scanner) {
        System.out.print("\nEnter 4 word numbers or words (or 'back'): ");
        return scanner.nextLine().trim();
    }

    public static List<String> renderGameBoard(Response infoResponse, ClientState state, Function<Long, Void> gameIdConsumer) {
        if (infoResponse == null || !infoResponse.success() || infoResponse.result() == null) {
            System.out.println("[!] Could not fetch puzzle details.");
            return List.of();
        }

        extractGameId(infoResponse.result()).ifPresent(gameIdConsumer::apply);

        List<String> allWords = extractWords(infoResponse.result());
        Set<String> allSolved = state.getAllSolvedWords();
        List<String> remainingWords = allWords.stream()
                .filter(w -> !allSolved.contains(w.toUpperCase()))
                .toList();

        System.out.println("==========================================");
        System.out.println("           CONNECTIONS BOARD");
        System.out.println("==========================================");

        List<Set<String>> solvedGroups = state.getSolvedGroups();
        if (!solvedGroups.isEmpty()) {
            System.out.println("Solved Groups:");
            IntStream.range(0, solvedGroups.size())
                    .forEach(i -> System.out.println("  Group " + (i + 1) + ": " + String.join(", ", solvedGroups.get(i))));
            System.out.println("------------------------------------------");
        }

        if (!remainingWords.isEmpty()) {
            System.out.println("Remaining Words:");
            IntStream.range(0, remainingWords.size())
                    .forEach(i -> System.out.printf("%2d) %-15s%s", (i + 1), remainingWords.get(i), (i + 1) % 4 == 0 ? "\n" : ""));
            if (remainingWords.size() % 4 != 0) System.out.println();
            System.out.println("------------------------------------------");
        }

        System.out.print("Mistakes remaining: ");
        int remainingMistakes = Math.max(0, state.getMaxMistakes() - state.getMistakesMade());
        IntStream.range(0, state.getMaxMistakes())
                .mapToObj(i -> i < state.getMistakesMade() ? "[X]" : "[ ]")
                .forEach(s -> System.out.print(s + " "));
        System.out.println("(" + remainingMistakes + " left)");
        System.out.println("==========================================");

        return remainingWords;
    }

    public static void printLeaderboard(Response response) {
        if (response == null || !response.success() || response.result() == null) {
            System.out.println("[!] Leaderboard unavailable.");
            return;
        }

        System.out.println("\n--- LEADERBOARD ---");
        String result = response.result();
        if (result.startsWith("POSITION:")) {
            System.out.println("Player Rank: " + result.replace("POSITION:", ""));
        } else {
            Arrays.stream(result.split(","))
                    .filter(s -> !s.isBlank())
                    .forEach(entry -> {
                        String[] parts = entry.split(":");
                        if (parts.length == 2) {
                            System.out.printf("• %-15s - %s wins\n", parts[0], parts[1]);
                        }
                    });
        }
    }

    public static void printPlayerStats(Response response) {
        if (response == null || !response.success() || response.result() == null) {
            System.out.println("[!] Stats unavailable.");
            return;
        }

        System.out.println("\n--- PLAYER STATS ---");
        Arrays.stream(response.result().split(","))
                .forEach(kv -> {
                    String[] p = kv.split(":");
                    if (p.length == 2) {
                        System.out.printf("%-15s: %s\n", p[0], p[1]);
                    }
                });
    }

    public static void printGameStats(Response response) {
        if (response == null || !response.success() || response.result() == null) {
            System.out.println("[!] Game stats unavailable.");
            return;
        }

        System.out.println("\n--- CURRENT GAME STATS ---");
        Arrays.stream(response.result().split(","))
                .forEach(kv -> {
                    String[] p = kv.split(":");
                    if (p.length == 2) {
                        System.out.printf("%-15s: %s\n", p[0], p[1]);
                    }
                });
    }

    private static Optional<Long> extractGameId(String resultText) {
        return Arrays.stream(resultText.split("\n"))
                .filter(line -> line.startsWith("GAME_ID:"))
                .map(line -> line.replace("GAME_ID:", "").trim())
                .map(Long::parseLong)
                .findFirst();
    }

    private static List<String> extractWords(String resultText) {
        return Arrays.stream(resultText.split("\n"))
                .filter(line -> line.startsWith("WORDS:"))
                .findFirst()
                .map(line -> line.replace("WORDS:", "").trim())
                .map(raw -> Arrays.stream(raw.split(",\\s*")).toList())
                .orElse(List.of());
    }

    public static void pauseForUser(Scanner scanner) {
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }
}