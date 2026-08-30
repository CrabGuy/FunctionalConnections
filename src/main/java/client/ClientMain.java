package client;

import shared.DataContracts;
import shared.Request;
import shared.Response;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ClientMain {
    private static final int UDP_PORT = 9876;

    private final NetworkClient networkClient;
    private String currentUser = null;
    private Long currentGameId = null;
    private final AtomicBoolean needsRefresh = new AtomicBoolean(false);

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
                    if (message.startsWith("GAME_UPDATE:") && currentUser != null) {
                        Long newGameId = Long.parseLong(message.replace("GAME_UPDATE:", ""));
                        if (currentGameId == null || !currentGameId.equals(newGameId)) {
                            needsRefresh.set(true);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("UDP Listener error: " + e.getMessage());
            }
        });
    }

    private void runAuthLoop(Scanner scanner) {
        while (currentUser == null) {
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
            if (currentUser == null) UiHandler.pauseForUser(scanner);
        }
        runGameLoop(scanner);
    }

    private void runGameLoop(Scanner scanner) {
        while (currentUser != null) {
            Response<DataContracts.GameStateDto> info = requestSilent(new Request.RequestGameInfo(null), DataContracts.GameStateDto.class);
            if (info.success() && info.result() != null) currentGameId = info.result().gameId();

            String option = UiHandler.showGameMenu(scanner, currentUser, info.result());

            // Check if a refresh was requested during UI interaction
            if (needsRefresh.getAndSet(false)) {
                continue; // go back to top and fetch fresh game info
            }

            switch (option) {
                case "1" -> interactivePlayLoop(scanner);
                case "2" -> fetchGameStats(scanner);
                case "3" -> fetchPlayerStats();
                case "4" -> fetchLeaderboard(scanner);
                case "5" -> handleUpdateCredentials(scanner, currentUser);
                case "6" -> handleLogout();
                default -> System.out.println("\n[!] Invalid selection.");
            }
            if (currentUser != null && !"1".equals(option)) UiHandler.pauseForUser(scanner);
        }
        runAuthLoop(scanner);
    }

    private void interactivePlayLoop(Scanner scanner) {
        String feedback = "";
        while (currentUser != null) {
            // Check for refresh before doing anything
            if (needsRefresh.getAndSet(false)) {
                UiHandler.clearScreen();
                System.out.println("\n[!] A new game has started! Returning to main menu...");
                UiHandler.pauseForUser(scanner);
                break;
            }

            UiHandler.clearScreen();
            Response<DataContracts.GameStateDto> info = requestSilent(new Request.RequestGameInfo(null), DataContracts.GameStateDto.class);
            if (!info.success() || info.result() == null) {
                System.out.println("✗ Could not load board: " + errorText(info));
                break;
            }
            DataContracts.GameStateDto board = info.result();
            currentGameId = board.gameId();

            UiHandler.renderGameBoard(board);
            if (!feedback.isBlank()) System.out.println("\n" + feedback);

            boolean isGameOver = "WON".equalsIgnoreCase(board.status()) || "LOST".equalsIgnoreCase(board.status());
            if (isGameOver) {
                System.out.println("WON".equalsIgnoreCase(board.status())
                        ? "\n🎉 CONGRATULATIONS! You solved all groups and WON this game!"
                        : "\n❌ GAME OVER! You ran out of attempts or time.");
                UiHandler.pauseForUser(scanner);
                break;
            }

            String input = UiHandler.promptProposal(scanner);
            // Check refresh again after input
            if (needsRefresh.getAndSet(false)) {
                UiHandler.clearScreen();
                System.out.println("\n[!] A new game has started! Returning to main menu...");
                UiHandler.pauseForUser(scanner);
                break;
            }

            if ("back".equalsIgnoreCase(input) || "exit".equalsIgnoreCase(input)) break;

            List<String> words = UiHandler.parseProposalInput(input, board.remainingWords());
            if (words.size() != 4) {
                feedback = "✗ Invalid input! Please provide 4 valid words or indices.";
                continue;
            }

            Response<DataContracts.ProposalOutcomeDto> outcome = requestSilent(new Request.SubmitProposal(words), DataContracts.ProposalOutcomeDto.class);
            if (outcome.success() && outcome.result() != null) {
                feedback = outcome.result().lastGuessCorrect() ? "✓ Correct group found!" : "✗ Incorrect group suggestion.";

                // Check if only one group remains and the user just solved one
                if (board.solvedGroups().size() == 2 && outcome.result().lastGuessCorrect()) {
                    if (UiHandler.confirmAutoSolve(scanner)) {
                        autoSolveLastGroup();
                    }
                }
            } else {
                feedback = "✗ Proposal Rejected: " + errorText(outcome);
            }
        }
    }

    private void autoSolveLastGroup() {
        Response<DataContracts.GameStateDto> info = requestSilent(new Request.RequestGameInfo(null), DataContracts.GameStateDto.class);
        if (info.success() && info.result() != null && info.result().remainingWords().size() == 4) {
            requestSilent(new Request.SubmitProposal(info.result().remainingWords()), DataContracts.ProposalOutcomeDto.class);
        }
    }

    private void fetchGameStats(Scanner scanner) {
        System.out.print("Enter game ID (press Enter for current game): ");
        String input = scanner.nextLine().trim();
        Long gameId = input.isBlank() ? null : Long.parseLong(input);
        Response<DataContracts.GameStatsDto> response = requestSilent(new Request.RequestGameStats(gameId), DataContracts.GameStatsDto.class);
        UiHandler.printGameStats(response.result());
    }

    private void fetchPlayerStats() {
        Response<DataContracts.PlayerStatsDto> response = requestSilent(new Request.RequestPlayerStats(), DataContracts.PlayerStatsDto.class);
        UiHandler.printPlayerStats(response.result());
    }

    private void fetchLeaderboard(Scanner scanner) {
        String targetPlayer = UiHandler.readTargetPlayer(scanner);
        Request request = targetPlayer.isBlank()
                ? new Request.RequestLeaderboard(null, 10)
                : new Request.RequestLeaderboard(targetPlayer, null);
        Response<DataContracts.LeaderboardDto> response = requestSilent(request, DataContracts.LeaderboardDto.class);
        UiHandler.printLeaderboard(response.result());
    }

    private void handleRegister(Scanner scanner) {
        UiHandler.readCredentials(scanner, "Register").ifPresent(credentials -> {
            Response<Void> response = requestSilent(new Request.Register(credentials.username(), credentials.password()), Void.class);
            if (response.success()) {
                System.out.println("✓ Registered successfully.");
                loginUser(credentials.username(), credentials.password());
            } else {
                System.out.println("✗ Registration failed: " + errorText(response));
            }
        });
    }

    private void handleLogin(Scanner scanner) {
        UiHandler.readCredentials(scanner, "Login").ifPresent(credentials -> loginUser(credentials.username(), credentials.password()));
    }

    private void loginUser(String username, String password) {
        Response<DataContracts.GameStateDto> response = requestSilent(new Request.Login(username, password), DataContracts.GameStateDto.class);
        if (response.success()) {
            currentUser = username;
            System.out.println("✓ Logged in as " + username);
        } else {
            System.out.println("✗ Login failed: " + errorText(response));
        }
    }

    private void handleLogout() {
        Response<Void> response = requestSilent(new Request.Logout(), Void.class);
        System.out.println(response.success() ? "✓ Logged out successfully." : "✗ Logout failed: " + errorText(response));
        currentUser = null;
        currentGameId = null;
    }

    private boolean handleUpdateCredentials(Scanner scanner, String currentUsername) {
        System.out.println("\n--- Update Credentials ---");
        String targetUsername = currentUsername == null || currentUsername.isBlank()
                ? UiHandler.promptText(scanner, "Username to update: ")
                : currentUsername;
        String oldPassword = UiHandler.promptText(scanner, "Current Password: ");
        String newUsernameInput = UiHandler.promptText(scanner, "New Username (press Enter to keep current): ");
        String newUsername = newUsernameInput.isBlank() ? targetUsername : newUsernameInput;
        String newPassword = UiHandler.promptText(scanner, "New Password: ");

        Response<Void> response = requestSilent(new Request.UpdateCredentials(targetUsername, oldPassword, newUsername, newPassword), Void.class);
        if (response.success()) {
            System.out.println("✓ Credentials updated.");
            if (currentUser != null) currentUser = newUsername;
            return true;
        }
        System.out.println("✗ Update failed: " + errorText(response));
        return false;
    }

    private <T> Response<T> requestSilent(Request request, Class<T> responseType) {
        try {
            return networkClient.sendRequest(request, responseType);
        } catch (Exception e) {
            return Response.error("Communication failure: " + e.getMessage());
        }
    }

    private String errorText(Response<?> response) {
        return response == null ? "Error" : ErrorDisplay.message(response.error());
    }
}