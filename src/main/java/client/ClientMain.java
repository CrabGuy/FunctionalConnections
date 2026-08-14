package client;

import shared.Request;
import shared.Response;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;

public final class ClientMain {
    private final NetworkClient networkClient;
    private final ClientState state;

    public ClientMain(String host, int port) throws IOException {
        this.networkClient = new NetworkClient(host, port);
        this.state = new ClientState();
    }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            ClientMain app = new ClientMain("localhost", 8080);
            app.runAuthLoop(scanner);
        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    private void runAuthLoop(Scanner scanner) {
        while (state.getCurrentUser() == null) {
            UiHandler.clearScreen();
            String choice = UiHandler.showAuthMenu(scanner);
            switch (choice) {
                case "1" -> {
                    UiHandler.clearScreen();
                    handleRegister(scanner);
                    UiHandler.pauseForUser(scanner);
                }
                case "2" -> {
                    UiHandler.clearScreen();
                    handleLogin(scanner);
                    UiHandler.pauseForUser(scanner);
                }
                case "3" -> {
                    UiHandler.clearScreen();
                    handleUpdateCredentials(scanner, null);
                    UiHandler.pauseForUser(scanner);
                }
                case "4" -> {
                    System.out.println("Goodbye!");
                    return;
                }
                default -> {
                    System.out.println("\n[!] Invalid option. Please try again.");
                    UiHandler.pauseForUser(scanner);
                }
            }
        }
        runGameLoop(scanner);
    }

    private void runGameLoop(Scanner scanner) {
        while (state.getCurrentUser() != null) {
            UiHandler.clearScreen();
            fetchGameInfo();
            String option = UiHandler.showGameMenu(scanner, state.getCurrentUser());

            switch (option) {
                case "1" -> interactivePlayLoop(scanner);
                case "2" -> {
                    UiHandler.clearScreen();
                    fetchGameInfo();
                    UiHandler.pauseForUser(scanner);
                }
                case "3" -> {
                    UiHandler.clearScreen();
                    fetchGameStats();
                    UiHandler.pauseForUser(scanner);
                }
                case "4" -> {
                    UiHandler.clearScreen();
                    fetchPlayerStats();
                    UiHandler.pauseForUser(scanner);
                }
                case "5" -> {
                    fetchLeaderboard(scanner);
                    UiHandler.pauseForUser(scanner);
                }
                case "6" -> {
                    UiHandler.clearScreen();
                    handleUpdateCredentials(scanner, state.getCurrentUser());
                    UiHandler.pauseForUser(scanner);
                }
                case "7" -> {
                    UiHandler.clearScreen();
                    handleLogout();
                    UiHandler.pauseForUser(scanner);
                }
                default -> {
                    System.out.println("\n[!] Invalid selection.");
                    UiHandler.pauseForUser(scanner);
                }
            }
        }
        runAuthLoop(scanner);
    }

    private void interactivePlayLoop(Scanner scanner) {
        String lastResultBanner = null;

        while (true) {
            UiHandler.clearScreen();
            System.out.println("==================================================");
            System.out.println("  === PLAY MODE (Type 'back' to return to menu) ===");

            if (lastResultBanner != null) {
                System.out.println(lastResultBanner);
                System.out.println("--------------------------------------------------");
            }

            fetchGameInfo();

            String input = UiHandler.promptProposal(scanner);
            if ("back".equalsIgnoreCase(input) || "exit".equalsIgnoreCase(input)) {
                break;
            }

            List<String> words = UiHandler.parseProposalInput(input);
            if (words.size() != 4) {
                lastResultBanner = "\n[!] Format Error: Enter exactly 4 distinct words separated by spaces or commas.";
                continue;
            }

            Request.SubmitProposal req = new Request.SubmitProposal("submitProposal", words);
            Optional<Response> responseOpt = executeRequest(req, "\n>>> Proposal Result", "\n>>> Proposal Rejected");

            if (responseOpt.isPresent()) {
                Response res = responseOpt.get();
                if (res.success()) {
                    processProposalSuccess(res, words);
                    lastResultBanner = "\n>>> Proposal Success: " + res.result();
                } else {
                    lastResultBanner = "\n>>> Proposal Rejected: " + res.error();
                }
            } else {
                lastResultBanner = "\n>>> Proposal Failed: No response from server";
            }
        }
    }

    private void processProposalSuccess(Response response, List<String> submittedWords) {
        if (response.result() != null && response.result().contains("LAST GUESS CORRECT: true")) {
            Set<String> upperWords = new HashSet<>(submittedWords.stream().map(String::toUpperCase).toList());
            state.addSolvedWords(upperWords);
        }
    }

    private void fetchGameInfo() {
        Request req = new Request.RequestGameInfo("gameInfo", null);
        Optional<Response> res = executeRequest(req, "", "Could not fetch game info");
        res.ifPresent(r -> UiHandler.printFilteredGameInfo(r, state.getSolvedWords(), id -> {
            state.updateGame(id);
            return null;
        }));
    }

    private void fetchGameStats() {
        executeRequest(new Request.RequestGameStats("gameStats", null), "Current Game Stats", "Could not fetch game stats");
    }

    private void fetchPlayerStats() {
        executeRequest(new Request.RequestPlayerStats("playerStats"), "Player Overall Stats", "Could not fetch player stats");
    }

    private void fetchLeaderboard(Scanner scanner) {
        UiHandler.clearScreen();
        System.out.println("=== LEADERBOARD ===");
        String targetPlayer = UiHandler.readTargetPlayer(scanner);
        System.out.println();

        Request req = targetPlayer.isBlank()
                ? new Request.RequestLeaderboard("leaderboard", null, 10)
                : new Request.RequestLeaderboard("leaderboard", targetPlayer, null);

        executeRequest(req, "Leaderboard Info", "Could not retrieve leaderboard");
    }

    private void handleRegister(Scanner scanner) {
        UiHandler.readCredentials(scanner, "Register")
                .flatMap(creds -> executeRequest(
                        new Request.Register("register", creds.username(), creds.password()),
                        "Registration", "Registration failed"
                ).filter(Response::success).flatMap(res -> loginUser(creds.username(), creds.password())));
    }

    private void handleLogin(Scanner scanner) {
        UiHandler.readCredentials(scanner, "Login")
                .ifPresent(creds -> loginUser(creds.username(), creds.password()));
    }

    private Optional<String> loginUser(String username, String password) {
        return executeRequest(
                new Request.Login("login", username, password),
                "Login Successful", "Login failed"
        ).filter(Response::success)
                .map(resp -> {
                    state.setCurrentUser(username);
                    return username;
                });
    }

    private void handleLogout() {
        executeRequest(new Request.Logout("logout"), "Logged out successfully.", "Logout failed");
        state.reset();
    }

    private boolean handleUpdateCredentials(Scanner scanner, String currentUsername) {
        System.out.println("\n--- Update Credentials ---");
        String targetUsername = currentUsername;

        if (targetUsername == null || targetUsername.isBlank()) {
            System.out.print("Username to update: ");
            targetUsername = scanner.nextLine().trim();
        }

        System.out.print("Current Password: ");
        String oldPsw = scanner.nextLine().trim();
        System.out.print("New Username (press Enter to keep current): ");
        String newUsernameInput = scanner.nextLine().trim();
        String newUsername = newUsernameInput.isBlank() ? targetUsername : newUsernameInput;
        System.out.print("New Password: ");
        String newPsw = scanner.nextLine().trim();

        Request.UpdateCredentials req = new Request.UpdateCredentials("updateCredentials", targetUsername, oldPsw, newUsername, newPsw);
        return executeRequest(req, "Credentials Update", "Update Failed")
                .map(Response::success)
                .orElse(false);
    }

    private Optional<Response> executeRequest(Request request, String successPrefix, String errorMsg) {
        try {
            Response response = networkClient.sendRequest(request);
            UiHandler.printResponse(response, successPrefix, errorMsg);
            return Optional.of(response);
        } catch (IOException e) {
            System.err.println("Communication error: " + e.getMessage());
            return Optional.empty();
        }
    }
}