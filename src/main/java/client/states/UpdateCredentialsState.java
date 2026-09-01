package client.states;

import client.ClientContext;
import client.ClientState;
import shared.Response;

import java.util.Scanner;

public final class UpdateCredentialsState implements ClientState {
    private final String currentUsername;

    public UpdateCredentialsState(String currentUsername) {
        this.currentUsername = currentUsername;
    }

    @Override
    public void handle(Scanner scanner, ClientContext context) {
        var view = context.getView();
        var session = context.getSession();

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
                System.out.println("Username changed. You are now logged in as " + newUsername);
            }
            // Stay logged in, go to main menu
            context.setState(new MainMenuState());
        } else {
            System.out.println("✗ Update failed: " + response.error());
            // Return to previous state? We'll just set AuthState if not logged in, else MainMenu
            if (session.isLoggedIn()) {
                context.setState(new MainMenuState());
            } else {
                context.setState(new AuthState());
            }
        }
    }
}