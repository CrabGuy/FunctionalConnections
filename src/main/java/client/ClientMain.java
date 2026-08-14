package client;

import shared.Request;
import shared.Response;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
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
                case "1" -> handleRegister(scanner);
                case "2" -> handleLogin(scanner);
                case "3" -> handleUpdateCredentials(scanner, null);
                case "4" -> {
                    System.out.println("See you tomorrow!");
                    System.exit(0);
                }
                default -> System.out.println("\n[!] Invalid option. Please try again.");
            }
            if (state.getCurrentUser() == null) {
                UiHandler.pauseForUser(scanner);
            }
        }
        runGameLoop(scanner);
    }

    private void runGameLoop(Scanner scanner) {
        while (state.getCurrentUser() != null) {
            UiHandler.clearScreen();
            fetchGameInfoSilently();
            String option = UiHandler.showGameMenu(scanner, state.getCurrentUser());
            switch (option) {
                case "1" -> interactivePlayLoop(scanner);
                case "2" -> fetchGameInfoSilently();
                case "3" -> fetchGameStats();
                case "4" -> fetchPlayerStats();
                case "5" -> fetchLeaderboard(scanner);
                case "6" -> handleUpdateCredentials(scanner, state.getCurrentUser());
                case "7" -> handleLogout();
                default -> System.out.println("\n[!] Invalid selection.");
            }
            if (state.getCurrentUser() != null && !"1".equals(option)) {
                UiHandler.pauseForUser(scanner);
            }
        }
        runAuthLoop(scanner);
    }

    private void interactivePlayLoop(Scanner scanner) {
        String feedback = "";
        while (true) {
            UiHandler.clearScreen();
            Response gameInfo = requestSilent(new Request.RequestGameInfo("gameInfo", null));
            List<String> availableWords = UiHandler.renderGameBoard(gameInfo, state, id -> {
                state.updateGame(id);
                return null;
            });

            syncGameStats();

            if (!feedback.isBlank()) {
                System.out.println("\n" + feedback);
            }

            if (state.isGameOver()) {
                if ("WON".equalsIgnoreCase(state.getStatus())) {
                    System.out.println("\n🎉 CONGRATULATIONS! You solved all groups and WON this game!");
                } else {
                    System.out.println("\n❌ GAME OVER! You ran out of attempts or time.");
                }
                UiHandler.pauseForUser(scanner);
                break;
            }

            String input = UiHandler.promptProposal(scanner);
            if ("back".equalsIgnoreCase(input) || "exit".equalsIgnoreCase(input)) {
                break;
            }

            List<String> words = UiHandler.parseProposalInput(input, availableWords);
            if (words.size() != 4) {
                feedback = "[!] Please select exactly 4 distinct words or numbers.";
                continue;
            }

            feedback = processProposal(words);

            if (state.getSolvedGroups().size() == 3 && !state.isGameOver()) {
                List<String> remainingWords = getRemainingWordsFromInfo();
                if (remainingWords.size() == 4) {
                    processProposal(remainingWords);
                }
            }
        }
    }

    private String processProposal(List<String> words) {
        Response res = requestSilent(new Request.SubmitProposal("submitProposal", words));
        if (res != null && res.success()) {
            if (res.result() != null && res.result().contains("LAST GUESS CORRECT: true")) {
                Set<String> upperWords = new HashSet<>(words.stream().map(String::toUpperCase).toList());
                state.addSolvedGroup(upperWords);
                syncGameStats();
                return "✓ Correct group found!";
            } else {
                syncGameStats();
                return "✗ Incorrect combination. Try again.";
            }
        } else {
            syncGameStats();
            return "✗ Proposal Rejected: " + (res != null ? res.error() : "No response");
        }
    }

    private List<String> getRemainingWordsFromInfo() {
        Response infoRes = requestSilent(new Request.RequestGameInfo("gameInfo", null));
        if (infoRes == null || !infoRes.success() || infoRes.result() == null) {
            return List.of();
        }
        Set<String> allSolved = state.getAllSolvedWords();
        return UiHandler.renderGameBoard(infoRes, state, id -> null).stream()
                .filter(w -> !allSolved.contains(w.toUpperCase()))
                .toList();
    }

    private void syncGameStats() {
        Response statsRes = requestSilent(new Request.RequestGameStats("gameStats", null));
        if (statsRes != null && statsRes.success() && statsRes.result() != null) {
            String[] parts = statsRes.result().split(",");
            for (String part : parts) {
                String[] kv = part.split(":");
                if (kv.length == 2) {
                    if ("STATUS".equals(kv[0])) state.setStatus(kv[1]);
                    if ("MISTAKES".equals(kv[0])) state.setMistakesMade(Integer.parseInt(kv[1]));
                }
            }
        }
    }

    private void fetchGameInfoSilently() {
        Response res = requestSilent(new Request.RequestGameInfo("gameInfo", null));
        if (res != null && res.success()) {
            UiHandler.renderGameBoard(res, state, id -> {
                state.updateGame(id);
                return null;
            });
        }
    }

    private void fetchGameStats() {
        Response res = requestSilent(new Request.RequestGameStats("gameStats", null));
        UiHandler.printGameStats(res);
    }

    private void fetchPlayerStats() {
        Response res = requestSilent(new Request.RequestPlayerStats("playerStats"));
        UiHandler.printPlayerStats(res);
    }

    private void fetchLeaderboard(Scanner scanner) {
        String targetPlayer = UiHandler.readTargetPlayer(scanner);
        Request req = targetPlayer.isBlank()
                ? new Request.RequestLeaderboard("leaderboard", null, 10)
                : new Request.RequestLeaderboard("leaderboard", targetPlayer, null);
        Response res = requestSilent(req);
        UiHandler.printLeaderboard(res);
    }

    private void handleRegister(Scanner scanner) {
        UiHandler.readCredentials(scanner, "Register")
                .ifPresent(creds -> {
                    Response res = requestSilent(new Request.Register("register", creds.username(), creds.password()));
                    if (res != null && res.success()) {
                        System.out.println("✓ Registered successfully.");
                        loginUser(creds.username(), creds.password());
                    } else {
                        System.out.println("✗ Registration failed: " + (res != null ? res.error() : "Error"));
                    }
                });
    }

    private void handleLogin(Scanner scanner) {
        UiHandler.readCredentials(scanner, "Login")
                .ifPresent(creds -> loginUser(creds.username(), creds.password()));
    }

    private void loginUser(String username, String password) {
        Response res = requestSilent(new Request.Login("login", username, password));
        if (res != null && res.success()) {
            state.setCurrentUser(username);
            System.out.println("✓ Logged in as " + username);
        } else {
            System.out.println("✗ Login failed: " + (res != null ? res.error() : "Error"));
        }
    }

    private void handleLogout() {
        Response res = requestSilent(new Request.Logout("logout"));
        if (res != null && res.success()) {
            System.out.println("✓ Logged out successfully.");
        }
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

        Request req = new Request.UpdateCredentials("updateCredentials", targetUsername, oldPsw, newUsername, newPsw);
        Response res = requestSilent(req);
        if (res != null && res.success()) {
            System.out.println("✓ Credentials updated.");
            if (state.getCurrentUser() != null) state.setCurrentUser(newUsername);
            return true;
        } else {
            System.out.println("✗ Update failed: " + (res != null ? res.error() : "Error"));
            return false;
        }
    }

    private Response requestSilent(Request request) {
        try {
            return networkClient.sendRequest(request);
        } catch (IOException e) {
            return new Response(false, null, "Communication failure: " + e.getMessage());
        }
    }
}