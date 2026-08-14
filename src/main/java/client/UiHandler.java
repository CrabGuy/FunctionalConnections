package client;

import shared.Response;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Function;

public final class UiHandler {
    public record Credentials(String username, String password) {}

    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static String showAuthMenu(Scanner scanner) {
        System.out.println("==========================================");
        System.out.println("          === CONNECTIONS GAME ===");
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
        System.out.println("1: Continuous Play Mode");
        System.out.println("2: Refresh Game Words & Info");
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
        System.out.print("Search specific player (press Enter for Top List): ");
        return scanner.nextLine().trim();
    }

    public static List<String> parseProposalInput(String input) {
        return Arrays.stream(input.split("[,\\s]+"))
                .filter(s -> !s.isBlank())
                .map(String::trim)
                .toList();
    }

    public static String promptProposal(Scanner scanner) {
        System.out.print("\nEnter 4 words (or 'back'): ");
        return scanner.nextLine().trim();
    }

    public static void printResponse(Response response, String successPrefix, String errorMsg) {
        Optional.ofNullable(response)
                .ifPresentOrElse(
                        res -> Optional.of(res)
                                .filter(Response::success)
                                .ifPresentOrElse(
                                        s -> System.out.println(successPrefix + (s.result() != null ? ":\n" + s.result() : "")),
                                        () -> System.out.println(errorMsg + ": " + res.error())
                                ),
                        () -> System.out.println(errorMsg + ": No response from server")
                );
    }

    public static void printFilteredGameInfo(Response response, Set<String> solvedWords, Function<Long, Void> gameIdConsumer) {
        if (response == null || !response.success() || response.result() == null) {
            printResponse(response, "Current Game Info", "Could not fetch game info");
            return;
        }

        extractGameId(response.result()).ifPresent(gameIdConsumer::apply);

        String filteredResult = Arrays.stream(response.result().split("\n"))
                .map(line -> line.startsWith("WORDS:") ? filterWordsLine(line, solvedWords) : line)
                .reduce((a, b) -> a + "\n" + b)
                .orElse(response.result());

        System.out.println("Current Game Info:\n" + filteredResult);
    }

    private static Optional<Long> extractGameId(String resultText) {
        return Arrays.stream(resultText.split("\n"))
                .filter(line -> line.startsWith("GAME_ID:"))
                .map(line -> line.replace("GAME_ID:", "").trim())
                .map(Long::parseLong)
                .findFirst();
    }

    private static String filterWordsLine(String wordsLine, Set<String> solvedWords) {
        String rawWords = wordsLine.replace("WORDS:", "").trim();
        List<String> remainingWords = Arrays.stream(rawWords.split(",\\s*"))
                .filter(w -> !solvedWords.contains(w.toUpperCase()))
                .toList();

        return "WORDS: " + String.join(", ", remainingWords);
    }

    public static void pauseForUser(Scanner scanner) {
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }
}