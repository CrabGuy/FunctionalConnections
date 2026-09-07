package client.ui;

import client.connection.ConnectionManager;
import client.formatting.GameEndReporter;
import client.formatting.OutputFormatter;
import client.session.AccountSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import shared.dto.*;

public final class LoggedInMenu {
    private final ConnectionManager connectionManager;
    private final AccountSession session;
    private final GameEndNotifier notifier;
    private final PrintStream output;
    private final BufferedReader input;

    public LoggedInMenu(
            ConnectionManager connectionManager,
            AccountSession session,
            GameEndNotifier notifier,
            PrintStream output,
            BufferedReader input) {
        this.connectionManager = connectionManager;
        this.session = session;
        this.notifier = notifier;
        this.output = output;
        this.input = input;
    }

    public void run() throws IOException {
        while (session.isLoggedIn()) {
            if (notifier.consumeIfPending()) {
                TerminalScreen.clear();
                output.println(
                        GameEndReporter.fetchAndFormat(
                                connectionManager, session.accountToken(), notifier.getGameId()));
                pressEnterToContinue();
                continue;
            }

            TerminalScreen.clear();
            output.println("Logged-in Menu");
            output.println("1. Play Game");
            output.println("2. Game Info");
            output.println("3. Game Stats");
            output.println("4. Leaderboard");
            output.println("5. Player Stats");
            output.println("6. Update Credentials");
            output.println("7. Logout");
            output.print("Choose an option: ");
            output.flush();

            String line = input.readLine();
            if (line == null) return;
            String choice = line.trim();

            switch (choice) {
                case "1" -> playGame();
                case "2" -> gameInfo();
                case "3" -> gameStats();
                case "4" -> leaderboard();
                case "5" -> playerStats();
                case "6" -> updateCredentials();
                case "7" -> logout();
                default -> output.println("Invalid option.");
            }
        }
    }

    private void playGame() throws IOException {
        GameLoop gameLoop = new GameLoop(connectionManager, session, notifier, output, input);
        gameLoop.run();
    }

    private void gameInfo() throws IOException {
        output.print("Enter game ID (or leave blank for current game): ");
        output.flush();
        String gameIdStr = input.readLine();
        Long gameId = gameIdStr.isBlank() ? null : Long.parseLong(gameIdStr);

        TerminalScreen.clear();
        ApiResponse<GameInfoData> response =
                ClientActions.fetchGameInfo(connectionManager, session.accountToken(), gameId);
        if (response.success()) {
            output.println(OutputFormatter.formatGameInfo(response.data(), System.currentTimeMillis()));
        } else {
            output.println("Error: " + response.error().message());
        }
        pressEnterToContinue();
    }

    private void gameStats() throws IOException {
        output.print("Enter game ID (or leave blank for current game): ");
        output.flush();
        String gameIdStr = input.readLine();
        Long gameId = gameIdStr.isBlank() ? null : Long.parseLong(gameIdStr);

        TerminalScreen.clear();
        ApiResponse<GameStatsData> response =
                ClientActions.fetchGameStats(connectionManager, session.accountToken(), gameId);
        if (response.success()) {
            output.println(OutputFormatter.formatGameStats(response.data(), System.currentTimeMillis()));
        } else {
            output.println("Error: " + response.error().message());
        }
        pressEnterToContinue();
    }

    private void leaderboard() throws IOException {
        output.println("1. Show top players");
        output.println("2. Show specific player");
        output.print("Choose: ");
        output.flush();
        String choice = input.readLine();
        String playerName = null;
        Integer topK = null;

        if (choice.equals("1")) {
            output.print("How many top players? (leave blank for all): ");
            output.flush();
            String topStr = input.readLine();
            if (!topStr.isBlank()) {
                topK = Integer.parseInt(topStr);
            }
        } else if (choice.equals("2")) {
            output.print("Enter player name: ");
            output.flush();
            playerName = input.readLine();
        } else {
            output.println("Invalid choice.");
            pressEnterToContinue();
            return;
        }

        TerminalScreen.clear();
        ApiResponse<LeaderboardData> response =
                ClientActions.fetchLeaderboard(
                        connectionManager, session.accountToken(), playerName, topK);
        if (response.success()) {
            output.println(OutputFormatter.formatLeaderboard(response.data()));
        } else {
            output.println("Error: " + response.error().message());
        }
        pressEnterToContinue();
    }

    private void playerStats() throws IOException {
        TerminalScreen.clear();
        ApiResponse<PlayerStatsData> response =
                ClientActions.fetchPlayerStats(connectionManager, session.accountToken());
        if (response.success()) {
            output.println(OutputFormatter.formatPlayerStats(response.data()));
        } else {
            output.println("Error: " + response.error().message());
        }
        pressEnterToContinue();
    }

    private void updateCredentials() throws IOException {
        output.print("Old username: ");
        output.flush();
        String oldUsername = input.readLine();
        output.print("New username: ");
        output.flush();
        String newUsername = input.readLine();
        output.print("Old password: ");
        output.flush();
        String oldPassword = input.readLine();
        output.print("New password: ");
        output.flush();
        String newPassword = input.readLine();

        TerminalScreen.clear();
        ClientActions.UpdateCredentialsResult result =
                ClientActions.updateCredentials(
                        connectionManager, oldUsername, newUsername, oldPassword, newPassword);
        if (result.success()) {
            output.println("Credentials updated successfully. You have been logged out.");
            session.clear();
        } else {
            output.println("Update failed: " + result.errorMessage());
        }
        pressEnterToContinue();
    }

    private void logout() throws IOException {
        TerminalScreen.clear();
        if (ClientActions.logout(connectionManager, session.accountToken())) {
            session.clear();
            output.println("Logged out successfully.");
        } else {
            output.println("Logout failed.");
        }
        pressEnterToContinue();
    }

    private void pressEnterToContinue() throws IOException {
        output.println();
        output.print("Press Enter to continue...");
        output.flush();
        input.readLine();
    }
}