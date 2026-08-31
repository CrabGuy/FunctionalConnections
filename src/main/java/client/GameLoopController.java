package client;

import shared.DataContracts;
import shared.Response;

import java.util.List;
import java.util.Scanner;

public final class GameLoopController {
    private final NetworkSession session;
    private final ConsoleView view;
    private final GameObserver observer;

    public GameLoopController(NetworkSession session, ConsoleView view, GameObserver observer) {
        this.session = session;
        this.view = view;
        this.observer = observer;
    }

    public void run(Scanner scanner) {
        while (true) {
            if (!session.isLoggedIn()) {
                runAuthLoop(scanner);
                if (!session.isLoggedIn()) break;
            }
            runGameLoop(scanner);
        }
    }

    private void runAuthLoop(Scanner scanner) {
        while (!session.isLoggedIn()) {
            String choice = view.showAuthMenu(scanner);
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
            if (!session.isLoggedIn()) view.pauseForUser(scanner);
        }
    }

    private void runGameLoop(Scanner scanner) {
        while (session.isLoggedIn()) {
            if (observer.hasNewGame()) {
                System.out.println("\n[!] A new game has started! Refreshing...");
            }
            Response<DataContracts.GameStateDto> infoResponse = session.getGameInfo(null);
            DataContracts.GameStateDto board = infoResponse.success() ? infoResponse.result() : null;
            String option = view.showGameMenu(scanner, session.getCurrentUser(), board);
            switch (option) {
                case "1" -> interactivePlayLoop(scanner);
                case "2" -> fetchGameStats(scanner);
                case "3" -> fetchPlayerStats();
                case "4" -> fetchLeaderboard(scanner);
                case "5" -> handleUpdateCredentials(scanner, session.getCurrentUser());
                case "6" -> handleLogout();
                default -> System.out.println("\n[!] Invalid selection.");
            }
            if (session.isLoggedIn() && !"1".equals(option)) view.pauseForUser(scanner);
        }
    }

    private void interactivePlayLoop(Scanner scanner) {
        String feedback = "";
        while (session.isLoggedIn()) {
            if (observer.hasNewGame()) {
                view.clearScreen();
                System.out.println("\n[!] A new game has started! Returning to main menu...");
                view.pauseForUser(scanner);
                break;
            }
            view.clearScreen();
            Response<DataContracts.GameStateDto> info = session.getGameInfo(null);
            if (!info.success() || info.result() == null) {
                System.out.println("✗ Could not load board: " + errorText(info));
                break;
            }
            DataContracts.GameStateDto board = info.result();
            view.renderGameBoard(board);
            if (!feedback.isBlank()) System.out.println("\n" + feedback);
            boolean isGameOver = "WON".equalsIgnoreCase(board.status()) || "LOST".equalsIgnoreCase(board.status());
            if (isGameOver) {
                System.out.println("WON".equalsIgnoreCase(board.status())
                        ? "\n🎉 CONGRATULATIONS! You solved all groups and WON this game!"
                        : "\n❌ GAME OVER! You ran out of attempts or time.");
                view.pauseForUser(scanner);
                break;
            }
            String input = view.promptProposal(scanner);
            if ("back".equalsIgnoreCase(input) || "exit".equalsIgnoreCase(input)) break;
            if (observer.hasNewGame()) {
                view.clearScreen();
                System.out.println("\n[!] A new game has started! Returning to main menu...");
                view.pauseForUser(scanner);
                break;
            }
            List<String> words = view.parseProposalInput(input, board.remainingWords());
            if (words.size() != 4) {
                feedback = "✗ Invalid input! Please provide 4 valid words or indices.";
                continue;
            }
            Response<DataContracts.ProposalOutcomeDto> outcome = session.submitProposal(words);
            if (outcome.success() && outcome.result() != null) {
                boolean correct = outcome.result().lastGuessCorrect();
                feedback = correct ? "✓ Correct group found!" : "✗ Incorrect group suggestion.";
                if (board.solvedGroups().size() == 2 && correct) {
                    if (view.confirmAutoSolve(scanner)) {
                        autoSolveLastGroup();
                    }
                }
            } else {
                feedback = "✗ Proposal Rejected: " + errorText(outcome);
            }
        }
    }

    private void autoSolveLastGroup() {
        Response<DataContracts.GameStateDto> info = session.getGameInfo(null);
        if (info.success() && info.result() != null && info.result().remainingWords().size() == 4) {
            session.submitProposal(info.result().remainingWords());
        }
    }

    private void fetchGameStats(Scanner scanner) {
        System.out.print("Enter game ID (press Enter for current game): ");
        String input = scanner.nextLine().trim();
        Long gameId = input.isBlank() ? null : Long.parseLong(input);
        Response<DataContracts.GameStatsDto> response = session.getGameStats(gameId);
        if (response.success()) {
            view.printGameStats(response.result());
        } else {
            System.out.println("✗ Failed to get game stats: " + errorText(response));
        }
    }

    private void fetchPlayerStats() {
        Response<DataContracts.PlayerStatsDto> response = session.getPlayerStats();
        if (response.success()) {
            view.printPlayerStats(response.result());
        } else {
            System.out.println("✗ Failed to get player stats: " + errorText(response));
        }
    }

    private void fetchLeaderboard(Scanner scanner) {
        String targetPlayer = view.readTargetPlayer(scanner);
        Integer topPlayers = targetPlayer.isBlank() ? 10 : null;
        Response<DataContracts.LeaderboardDto> response = session.getLeaderboard(targetPlayer, topPlayers);
        if (response.success()) {
            view.printLeaderboard(response.result());
        } else {
            System.out.println("✗ Failed to get leaderboard: " + errorText(response));
        }
    }

    private void handleRegister(Scanner scanner) {
        view.readCredentials(scanner, "Register").ifPresent(credentials -> {
            Response<Void> response = session.register(credentials.username(), credentials.password());
            if (response.success()) {
                System.out.println("✓ Registered successfully.");
                session.login(credentials.username(), credentials.password());
            } else {
                System.out.println("✗ Registration failed: " + errorText(response));
            }
        });
    }

    private void handleLogin(Scanner scanner) {
        view.readCredentials(scanner, "Login").ifPresent(credentials -> {
            Response<DataContracts.GameStateDto> response = session.login(credentials.username(), credentials.password());
            if (response.success()) {
                System.out.println("✓ Logged in as " + credentials.username());
            } else {
                System.out.println("✗ Login failed: " + errorText(response));
            }
        });
    }

    private void handleLogout() {
        Response<Void> response = session.logout();
        System.out.println(response.success() ? "✓ Logged out successfully." : "✗ Logout failed: " + errorText(response));
    }

    private boolean handleUpdateCredentials(Scanner scanner, String currentUsername) {
        System.out.println("\n--- Update Credentials ---");
        String targetUsername = currentUsername == null || currentUsername.isBlank()
                ? view.promptText(scanner, "Username to update: ")
                : currentUsername;
        String oldPassword = view.promptText(scanner, "Current Password: ");
        String newUsernameInput = view.promptText(scanner, "New Username (press Enter to keep current): ");
        String newUsername = newUsernameInput.isBlank() ? targetUsername : newUsernameInput;
        String newPassword = view.promptText(scanner, "New Password: ");
        Response<Void> response = session.updateCredentials(targetUsername, oldPassword, newUsername, newPassword);
        if (response.success()) {
            System.out.println("✓ Credentials updated.");
            if (currentUsername != null && !newUsername.equals(currentUsername)) {
                // session will update on next login
            }
            return true;
        } else {
            System.out.println("✗ Update failed: " + errorText(response));
            return false;
        }
    }

    private String errorText(Response<?> response) {
        return response == null ? "Error" : ErrorDisplay.message(response.error());
    }
}