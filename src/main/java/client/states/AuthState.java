package client.states;

import client.ClientContext;
import client.ClientState;
import shared.DataContracts;
import shared.Response;

import java.util.Scanner;

public final class AuthState implements ClientState {
    @Override
    public void handle(Scanner scanner, ClientContext context) {
        var view = context.getView();
        var session = context.getSession();

        while (!session.isLoggedIn() && !context.isExitRequested()) {
            String choice = view.showAuthMenu(scanner);
            switch (choice) {
                case "1" -> handleRegister(scanner, context);
                case "2" -> handleLogin(scanner, context);
                case "3" -> handleUpdateCredentials(scanner, context, null);
                case "4" -> {
                    System.out.println("See you tomorrow!");
                    context.requestExit();
                }
                default -> System.out.println("\n[!] Invalid option. Please try again.");
            }
            if (!session.isLoggedIn() && !context.isExitRequested()) {
                view.pauseForUser(scanner);
            }
        }
        if (session.isLoggedIn()) {
            context.setState(new MainMenuState());
        }
    }

    private void handleRegister(Scanner scanner, ClientContext context) {
        var view = context.getView();
        var session = context.getSession();
        view.readCredentials(scanner, "Register").ifPresent(credentials -> {
            Response<Void> response = session.register(credentials.username(), credentials.password());
            if (response.success()) {
                System.out.println("✓ Registered successfully.");
                session.login(credentials.username(), credentials.password());
            } else {
                System.out.println("✗ Registration failed: " + response.error());
            }
        });
    }

    private void handleLogin(Scanner scanner, ClientContext context) {
        var view = context.getView();
        var session = context.getSession();
        view.readCredentials(scanner, "Login").ifPresent(credentials -> {
            Response<DataContracts.GameStateDto> response = session.login(credentials.username(), credentials.password());
            if (response.success()) {
                System.out.println("✓ Logged in as " + credentials.username());
            } else {
                System.out.println("✗ Login failed: " + response.error());
            }
        });
    }

    private void handleUpdateCredentials(Scanner scanner, ClientContext context, String currentUsername) {
        context.setState(new UpdateCredentialsState(currentUsername));
        context.getState().handle(scanner, context);
    }
}