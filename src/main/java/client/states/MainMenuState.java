package client.states;

import client.ClientContext;
import client.ClientState;
import shared.DataContracts;
import shared.Response;

import java.util.Scanner;

public final class MainMenuState implements ClientState {
    @Override
    public void handle(Scanner scanner, ClientContext context) {
        var view = context.getView();
        var session = context.getSession();
        var observer = context.getObserver();

        if (observer.hasNewGame()) {
            System.out.println("\n[!] A new game has started! Refreshing...");
        }

        Response<DataContracts.GameStateDto> infoResponse = session.getGameInfo(null);
        DataContracts.GameStateDto board = infoResponse.success() ? infoResponse.result() : null;

        String option = view.showGameMenu(scanner, session.getCurrentUser(), board);
        switch (option) {
            case "1" -> context.setState(new PlayingState());
            case "2" -> fetchGameStats(scanner, context);
            case "3" -> fetchPlayerStats(context);
            case "4" -> fetchLeaderboard(scanner, context);
            case "5" -> {
                context.setState(new UpdateCredentialsState(session.getCurrentUser()));
                context.getState().handle(scanner, context);
                if (context.getState() instanceof AuthState) {
                    // logged out? no, but if credentials changed maybe still logged in.
                }
            }
            case "6" -> handleLogout(context);
            default -> System.out.println("\n[!] Invalid selection.");
        }

        if (session.isLoggedIn() && !"1".equals(option) && !(context.getState() instanceof PlayingState)) {
            view.pauseForUser(scanner);
        }
    }

    private void fetchGameStats(Scanner scanner, ClientContext context) {
        System.out.print("Enter game ID (press Enter for current game): ");
        String input = scanner.nextLine().trim();
        Long gameId = null;
        if (!input.isBlank()) {
            try {
                gameId = Long.parseLong(input);
            } catch (NumberFormatException e) {
                System.out.println("✗ Invalid game ID format.");
                return;
            }
        }
        Response<DataContracts.GameStatsDto> response = context.getSession().getGameStats(gameId);
        if (response.success()) {
            context.getView().printGameStats(response.result());
        } else {
            System.out.println("✗ Failed to get game stats: " + response.error());
        }
    }

    private void fetchPlayerStats(ClientContext context) {
        Response<DataContracts.PlayerStatsDto> response = context.getSession().getPlayerStats();
        if (response.success()) {
            context.getView().printPlayerStats(response.result());
        } else {
            System.out.println("✗ Failed to get player stats: " + response.error());
        }
    }

    private void fetchLeaderboard(Scanner scanner, ClientContext context) {
        String targetPlayer = context.getView().readTargetPlayer(scanner);
        Integer topPlayers = targetPlayer.isBlank() ? 10 : null;
        Response<DataContracts.LeaderboardDto> response = context.getSession().getLeaderboard(targetPlayer, topPlayers);
        if (response.success()) {
            context.getView().printLeaderboard(response.result());
        } else {
            System.out.println("✗ Failed to get leaderboard: " + response.error());
        }
    }

    private void handleLogout(ClientContext context) {
        Response<Void> response = context.getSession().logout();
        System.out.println(response.success() ? "✓ Logged out successfully." : "✗ Logout failed: " + response.error());
        context.setState(new AuthState());
    }
}