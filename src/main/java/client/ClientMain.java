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
    private ClientState state = ClientState.empty();
    private volatile boolean needsRefresh = false;

    private record ProposalOutcome(String feedback, ClientState state) {}

    public ClientMain(String host, int port) throws Exception {
        this.networkClient = new NetworkClient(host, port);
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
                    if (message.startsWith("GAME_UPDATE:") && state.currentUser() != null) {
                        Long newGameId = Long.parseLong(message.replace("GAME_UPDATE:", ""));
                        if (state.currentGameId() == null || !state.currentGameId().equals(newGameId)) {
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
        while (state.currentUser() == null) {
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
            if (state.currentUser() == null) {
                UiHandler.pauseForUser(scanner);
            }
        }
        runGameLoop(scanner);
    }

    private void runGameLoop(Scanner scanner) {
        while (state.currentUser() != null) {
            needsRefresh = false;
            Response gameInfo = requestSilent(new Request.RequestGameState("requestGameState", null));
            UiHandler.BoardView board = UiHandler.parseGameBoard(gameInfo, state);
            if (board.gameId() != null) {
                state = state.withGame(board.gameId());
            }
            state = syncGameState(gameInfo, state);
            String option = UiHandler.showGameMenu(scanner, state.currentUser(), gameInfo, state);
            if (needsRefresh) {
                continue;
            }
            switch (option) {
                case "1" -> interactivePlayLoop(scanner);
                case "2" -> fetchGameStats(scanner);
                case "3" -> fetchPlayerStats();
                case "4" -> fetchLeaderboard(scanner);
                case "5" -> handleUpdateCredentials(scanner, state.currentUser());
                case "6" -> handleLogout();
                default -> System.out.println("\n[!] Invalid selection.");
            }
            if (state.currentUser() != null && !"1".equals(option)) {
                UiHandler.pauseForUser(scanner);
            }
        }
        runAuthLoop(scanner);
    }

    private void interactivePlayLoop(Scanner scanner) {
        String feedback = "";
        while (state.currentUser() != null) {
            if (needsRefresh) {
                UiHandler.clearScreen();
                System.out.println("\n[!] A new game has started! Returning to main menu...");
                UiHandler.pauseForUser(scanner);
                break;
            }
            UiHandler.clearScreen();
            Response gameInfo = requestSilent(new Request.RequestGameState("requestGameState", null));
            UiHandler.BoardView board = UiHandler.parseGameBoard(gameInfo, state);
            if (board.gameId() != null) {
                state = state.withGame(board.gameId());
            }
            state = syncGameState(gameInfo, state);
            UiHandler.renderGameBoard(gameInfo, state);
            if (!feedback.isBlank()) {
                System.out.println("\n" + feedback);
            }
            if (state.isGameOver()) {
                if ("WON".equalsIgnoreCase(state.status())) {
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
            List<String> words = UiHandler.parseProposalInput(input, board.remainingWords());
            if (words.size() != 4) {
                feedback = "✗ Invalid input! Please provide 4 valid words or indices.";
                continue;
            }
            ProposalOutcome outcome = processProposal(words, state);
            state = outcome.state();
            feedback = outcome.feedback();
            if (state.solvedGroups().size() == 3 && !state.isGameOver()) {
                List<String> remainingWords = getRemainingWordsFromInfo();
                if (remainingWords.size() == 4) {
                    ProposalOutcome autoOutcome = processProposal(remainingWords, state);
                    state = autoOutcome.state();
                    feedback = autoOutcome.feedback();
                }
            }
        }
    }

    private ProposalOutcome processProposal(List<String> words, ClientState currentState) {
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
                ClientState updated = currentState.withSolvedGroup(upperWords);
                if (status != null) {
                    updated = updated.withStatus(status);
                }
                return new ProposalOutcome("✓ Correct group found!", updated);
            }
            ClientState updated = currentState.withMistakesMade(currentState.mistakesMade() + 1);
            if (status != null) {
                updated = updated.withStatus(status);
            }
            return new ProposalOutcome("✗ Incorrect group suggestion.", updated);
        }
        return new ProposalOutcome("✗ Proposal Rejected: " + errorText(response), currentState);
    }

    private List<String> getRemainingWordsFromInfo() {
        Response infoResponse = requestSilent(new Request.RequestGameState("requestGameState", null));
        if (infoResponse == null || !infoResponse.success() || infoResponse.result() == null) {
            return List.of();
        }
        UiHandler.BoardView board = UiHandler.parseGameBoard(infoResponse, state);
        if (board.gameId() != null) {
            state = state.withGame(board.gameId());
        }
        state = syncGameState(infoResponse, state);
        Set<String> allSolved = state.getAllSolvedWords();
        return board.remainingWords().stream()
                .filter(word -> !allSolved.contains(word.toUpperCase()))
                .toList();
    }

    private ClientState syncGameState(Response gameInfo, ClientState currentState) {
        if (gameInfo == null || !gameInfo.success() || gameInfo.result() == null) {
            return currentState;
        }
        String[] lines = gameInfo.result().split("\\R");
        ClientState updated = currentState;
        for (String line : lines) {
            String[] kv = line.split(":", 2);
            if (kv.length < 2) {
                continue;
            }
            if ("STATUS".equals(kv[0])) {
                updated = updated.withStatus(kv[1].trim());
            }
            if ("MISTAKES".equals(kv[0])) {
                updated = updated.withMistakesMade(Integer.parseInt(kv[1].trim()));
            }
        }
        return updated;
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
                        System.out.println("✗ Registration failed: " + errorText(response));
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
            state = state.withCurrentUser(username);
            System.out.println("✓ Logged in as " + username);
        } else {
            System.out.println("✗ Login failed: " + errorText(response));
        }
    }

    private void handleLogout() {
        Response response = requestSilent(new Request.Logout("logout"));
        if (response != null && response.success()) {
            System.out.println("✓ Logged out successfully.");
        } else {
            System.out.println("✗ Logout failed: " + errorText(response));
        }
        state = state.withReset();
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
            if (state.currentUser() != null) {
                state = state.withCurrentUser(newUsername);
            }
            return true;
        } else {
            System.out.println("✗ Update failed: " + errorText(response));
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

    private String errorText(Response response) {
        if (response == null) {
            return "Error";
        }
        return ErrorDisplay.message(response.error());
    }
}