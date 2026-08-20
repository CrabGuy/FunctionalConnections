package client;

import shared.Request;
import shared.Response;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public final class ClientMain {
    private static final int UDP_PORT = 9876;
    private final NetworkClient networkClient;
    private final ClientState state;
    private volatile boolean needsRefresh = false;

    public ClientMain(String host, int port) throws Exception {
        this.networkClient = new NetworkClient(host, port);
        this.state = new ClientState();
    }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            ClientMain app = new ClientMain("localhost", 8080);
            app.startUdpListener();
            app.runAuthLoop(scanner);
        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    private void startUdpListener() {
        Thread.ofPlatform().daemon().start(() -> {
            try (DatagramSocket socket = new DatagramSocket(UDP_PORT)) {
                byte[] buffer = new byte[1024];
                while (!Thread.currentThread().isInterrupted()) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();
                    if (message.startsWith("GAME_UPDATE:") && state.getCurrentUser() != null) {
                        Long newGameId = Long.parseLong(message.replace("GAME_UPDATE:", ""));
                        if (state.getCurrentGameId() == null || !state.getCurrentGameId().equals(newGameId)) {
                            needsRefresh = true;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("UDP Listener error: " + e.getMessage());
            }
        });
    }

    private void runAuthLoop(Scanner scanner) {
        while (state.getCurrentUser() == null) {
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
            needsRefresh = false;
            Response gameInfo = requestSilent(new Request.RequestGameState("requestGameState", null));
            String option = UiHandler.showGameMenu(scanner, state.getCurrentUser(), gameInfo, state);
            if (needsRefresh) {
                continue;
            }
            switch (option) {
                case "1" -> interactivePlayLoop(scanner);
                case "2" -> fetchGameStats(scanner);
                case "3" -> fetchPlayerStats();
                case "4" -> fetchLeaderboard(scanner);
                case "5" -> handleUpdateCredentials(scanner, state.getCurrentUser());
                case "6" -> handleLogout();
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
        while (state.getCurrentUser() != null) {
            if (needsRefresh) {
                UiHandler.clearScreen();
                System.out.println("\n[!] A new game has started! Returning to main menu...");
                UiHandler.pauseForUser(scanner);
                break;
            }
            UiHandler.clearScreen();
            Response gameInfo = requestSilent(new Request.RequestGameState("requestGameState", null));
            List<String> availableWords = UiHandler.renderGameBoard(gameInfo, state, state::updateGame);
            syncGameState(gameInfo);
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
            if (needsRefresh) {
                UiHandler.clearScreen();
                System.out.println("\n[!] A new game has started! Returning to main menu...");
                UiHandler.pauseForUser(scanner);
                break;
            }
            if ("back".equalsIgnoreCase(input) || "exit".equalsIgnoreCase(input)) {
                break;
            }
            List<String> words = UiHandler.parseProposalInput(input, availableWords);
            if (words.size() != 4) {
                feedback = "✗ Invalid input! Please provide 4 valid words or indices.";
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
        Response response = requestSilent(new Request.SendAnswer("sendAnswer", words));
        if (response != null && response.success()) {
            String result = response.result();
            String status = null;
            boolean lastGuessCorrect = false;
            if (result != null) {
                String[] parts = result.split("\\|");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (trimmed.startsWith("STATUS:")) {
                        status = trimmed.substring("STATUS:".length()).trim();
                    } else if (trimmed.startsWith("LAST GUESS CORRECT:")) {
                        lastGuessCorrect = Boolean.parseBoolean(trimmed.substring("LAST GUESS CORRECT:".length()).trim());
                    }
                }
            }
            if (lastGuessCorrect) {
                Set<String> upperWords = new HashSet<>(words.stream().map(String::toUpperCase).toList());
                state.addSolvedGroup(upperWords);
                if (status != null) state.setStatus(status);
                return "✓ Correct group found!";
            }
            state.setMistakesMade(state.getMistakesMade() + 1);
            if (status != null) state.setStatus(status);
            return "✗ Incorrect group suggestion.";
        }
        return "✗ Proposal Rejected: " + (response != null ? response.error() : "No response");
    }

    private List<String> getRemainingWordsFromInfo() {
        Response infoResponse = requestSilent(new Request.RequestGameState("requestGameState", null));
        if (infoResponse == null || !infoResponse.success() || infoResponse.result() == null) {
            return List.of();
        }
        Set<String> allSolved = state.getAllSolvedWords();
        return UiHandler.renderGameBoard(infoResponse, state, id -> {}).stream()
                .filter(word -> !allSolved.contains(word.toUpperCase()))
                .toList();
    }

    private void syncGameState(Response gameInfo) {
        if (gameInfo == null || !gameInfo.success() || gameInfo.result() == null) {
            return;
        }
        String[] lines = gameInfo.result().split("\\R");
        for (String line : lines) {
            String[] kv = line.split(":", 2);
            if (kv.length < 2) {
                continue;
            }
            if ("STATUS".equals(kv[0])) {
                state.setStatus(kv[1].trim());
            }
            if ("MISTAKES".equals(kv[0])) {
                state.setMistakesMade(Integer.parseInt(kv[1].trim()));
            }
        }
    }

    private void fetchGameStats(Scanner scanner) {
        System.out.print("Enter game ID (press Enter for current game): ");
        String input = scanner.nextLine().trim();
        Long gameId = input.isBlank() ? null : Long.parseLong(input);
        Response response = requestSilent(new Request.RequestGameStatistics("requestGameStatistics", gameId));
        UiHandler.printGameStats(response);
    }

    private void fetchPlayerStats() {
        Response response = requestSilent(new Request.RequestPersonalStats("requestPersonalStats"));
        UiHandler.printPlayerStats(response);
    }

    private void fetchLeaderboard(Scanner scanner) {
        String targetPlayer = UiHandler.readTargetPlayer(scanner);
        Request request = targetPlayer.isBlank()
                ? new Request.RequestLeaderboardInfo("requestLeaderboardInfo", null, 10)
                : new Request.RequestLeaderboardInfo("requestLeaderboardInfo", targetPlayer, null);
        Response response = requestSilent(request);
        UiHandler.printLeaderboard(response);
    }

    private void handleRegister(Scanner scanner) {
        UiHandler.readCredentials(scanner, "Register")
                .ifPresent(credentials -> {
                    Response response = requestSilent(new Request.Signup("signup", credentials.username(), credentials.password()));
                    if (response != null && response.success()) {
                        System.out.println("✓ Registered successfully.");
                        loginUser(credentials.username(), credentials.password());
                    } else {
                        System.out.println("✗ Registration failed: " + (response != null ? response.error() : "Error"));
                    }
                });
    }

    private void handleLogin(Scanner scanner) {
        UiHandler.readCredentials(scanner, "Login")
                .ifPresent(credentials -> loginUser(credentials.username(), credentials.password()));
    }

    private void loginUser(String username, String password) {
        Response response = requestSilent(new Request.Login("login", username, password));
        if (response != null && response.success()) {
            state.setCurrentUser(username);
            System.out.println("✓ Logged in as " + username);
        } else {
            System.out.println("✗ Login failed: " + (response != null ? response.error() : "Error"));
        }
    }

    private void handleLogout() {
        Response response = requestSilent(new Request.Logout("logout"));
        if (response != null && response.success()) {
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
        String oldPassword = scanner.nextLine().trim();
        System.out.print("New Username (press Enter to keep current): ");
        String newUsernameInput = scanner.nextLine().trim();
        String newUsername = newUsernameInput.isBlank() ? targetUsername : newUsernameInput;
        System.out.print("New Password: ");
        String newPassword = scanner.nextLine().trim();
        Response response = requestSilent(new Request.UpdateCredentials(
                "updateCredentials",
                targetUsername,
                oldPassword,
                newUsername,
                newPassword
        ));
        if (response != null && response.success()) {
            System.out.println("✓ Credentials updated.");
            if (state.getCurrentUser() != null) {
                state.setCurrentUser(newUsername);
            }
            return true;
        } else {
            System.out.println("✗ Update failed: " + (response != null ? response.error() : "Error"));
            return false;
        }
    }

    private Response requestSilent(Request request) {
        try {
            return networkClient.sendRequest(request);
        } catch (Exception e) {
            return new Response(false, null, "Communication failure: " + e.getMessage());
        }
    }
}