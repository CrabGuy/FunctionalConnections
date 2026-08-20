package client;

import shared.Response;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public final class UiHandler {
    public record Credentials(String username, String password) {}

    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static String showAuthMenu(Scanner scanner) {
        clearScreen();
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

    public static void renderGameMenuHeader(String username, Response gameInfo, ClientState state) {
        clearScreen();
        System.out.println("==========================================");
        System.out.println("      --- MAIN MENU (" + username + ") ---");
        System.out.println("==========================================");
        renderGameBoard(gameInfo, state, state::updateGame);
        System.out.println("\n------------------------------------------");
        System.out.println("1: Play Active Game");
        System.out.println("2: Game Statistics");
        System.out.println("3: Player Overall Stats");
        System.out.println("4: Leaderboards");
        System.out.println("5: Update Credentials");
        System.out.println("6: Logout");
        System.out.print("Choose option: ");
    }

    public static String showGameMenu(Scanner scanner, String username, Response gameInfo, ClientState state) {
        renderGameMenuHeader(username, gameInfo, state);
        return scanner.nextLine().trim();
    }

    public static Optional<Credentials> readCredentials(Scanner scanner, String action) {
        clearScreen();
        System.out.println("--- " + action + " ---");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();
        return Optional.of(new Credentials(username, password))
                .filter(credentials -> !credentials.username().isBlank() && !credentials.password().isBlank());
    }

    public static String readTargetPlayer(Scanner scanner) {
        System.out.print("Search specific player (press Enter for Top 10): ");
        return scanner.nextLine().trim();
    }

    public static List<String> parseProposalInput(String input, List<String> availableWords) {
        List<String> rawTokens = Arrays.stream(input.split("[,\\s]+"))
                .filter(token -> !token.isBlank())
                .map(String::trim)
                .toList();
        if (rawTokens.isEmpty()) {
            return List.of();
        }
        boolean isNumeric = rawTokens.stream().allMatch(token -> token.matches("\\d+"));
        if (isNumeric) {
            return rawTokens.stream()
                    .map(Integer::parseInt)
                    .filter(index -> index >= 1 && index <= availableWords.size())
                    .map(index -> availableWords.get(index - 1))
                    .distinct()
                    .toList();
        }
        return rawTokens.stream().distinct().toList();
    }

    public static String promptProposal(Scanner scanner) {
        System.out.print("\nEnter 4 word numbers or words (or 'back'): ");
        return scanner.nextLine().trim();
    }

    public static List<String> renderGameBoard(Response infoResponse, ClientState state, Consumer<Long> gameIdConsumer) {
        if (infoResponse == null || !infoResponse.success() || infoResponse.result() == null) {
            System.out.println("[!] Could not fetch puzzle details.");
            return List.of();
        }
        extractGameId(infoResponse.result()).ifPresent(gameIdConsumer::accept);
        List<String> allWords = extractWords(infoResponse.result());
        Set<String> allSolved = state.getAllSolvedWords();
        List<String> remainingWords = allWords.stream()
                .filter(word -> !allSolved.contains(word.toUpperCase()))
                .toList();

        System.out.println("\n           CONNECTIONS BOARD");
        System.out.println("==========================================");
        List<Set<String>> solvedGroups = state.getSolvedGroups();
        if (!solvedGroups.isEmpty()) {
            System.out.println("Solved Groups:");
            IntStream.range(0, solvedGroups.size())
                    .forEach(index -> System.out.println("  Group " + (index + 1) + ": " + String.join(", ", solvedGroups.get(index))));
            System.out.println("------------------------------------------");
        }
        if (!remainingWords.isEmpty()) {
            System.out.println("Remaining Words:");
            IntStream.range(0, remainingWords.size())
                    .forEach(index -> System.out.printf("%2d) %-15s%s", (index + 1), remainingWords.get(index), (index + 1) % 4 == 0 ? "\n" : ""));
            if (remainingWords.size() % 4 != 0) {
                System.out.println();
            }
            System.out.println("------------------------------------------");
        }
        System.out.print("Mistakes remaining: ");
        int remainingMistakes = Math.max(0, state.getMaxMistakes() - state.getMistakesMade());
        IntStream.range(0, state.getMaxMistakes())
                .mapToObj(index -> index < state.getMistakesMade() ? "[X]" : "[ ]")
                .forEach(mark -> System.out.print(mark + " "));
        System.out.println("(" + remainingMistakes + " left)");
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
            return;
        }
        Arrays.stream(result.split(","))
                .filter(entry -> entry.contains(":"))
                .forEach(entry -> {
                    String[] parts = entry.split(":");
                    if (parts.length >= 2) {
                        System.out.printf("• %-15s - %s wins\n", parts[0], parts[1]);
                    }
                });
    }

    public static void printPlayerStats(Response response) {
        if (response == null || !response.success() || response.result() == null) {
            System.out.println("[!] Stats unavailable.");
            return;
        }
        System.out.println("\n--- PLAYER STATS ---");
        String result = response.result();
        for (String line : result.split("\\R")) {
            int colonIndex = line.indexOf(':');
            if (colonIndex < 0) {
                continue;
            }
            String key = line.substring(0, colonIndex);
            String value = line.substring(colonIndex + 1);
            if ("MISTAKE_HISTOGRAM".equals(key)) {
                System.out.println("Mistake Histogram (mistakes -> games):");
                if ("NONE".equals(value)) {
                    System.out.println("  No completed games yet.");
                } else {
                    Arrays.stream(value.split(","))
                            .filter(entry -> entry.contains(":"))
                            .forEach(entry -> {
                                String[] histParts = entry.split(":");
                                if (histParts.length >= 2) {
                                    System.out.printf("  %s mistakes: %s game(s)\n", histParts[0], histParts[1]);
                                }
                            });
                }
            } else {
                System.out.printf("%-20s: %s\n", key, value);
            }
        }
    }

    public static void printGameStats(Response response) {
        if (response == null || !response.success() || response.result() == null) {
            System.out.println("[!] Game stats unavailable.");
            return;
        }
        System.out.println("\n--- GAME STATISTICS ---");
        for (String line : response.result().split("\\R")) {
            int colonIndex = line.indexOf(':');
            if (colonIndex < 0) {
                continue;
            }
            String key = line.substring(0, colonIndex);
            String value = line.substring(colonIndex + 1);
            System.out.printf("%-20s: %s\n", key, value);
        }
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
                .filter(line -> line.startsWith("REMAINING_WORDS:"))
                .findFirst()
                .map(line -> line.replace("REMAINING_WORDS:", "").trim())
                .map(raw -> Arrays.stream(raw.split(",\\s*"))
                        .filter(word -> !word.isBlank())
                        .toList())
                .orElse(List.of());
    }

    public static void pauseForUser(Scanner scanner) {
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }
}