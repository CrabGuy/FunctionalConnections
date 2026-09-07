package client.ui;

import client.connection.ConnectionManager;
import client.notification.NotificationListener;
import client.session.AccountSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Displays the main menu for unauthenticated users, allowing login, registration, credential
 * updates, help, and exit.
 */
public final class MainMenu {

  private final ConnectionManager connectionManager;
  private final AccountSession session;
  private final NotificationListener notificationListener;
  private final GameEndNotifier notifier;
  private final PrintStream output;
  private final BufferedReader input;
  private final int udpPort;

  /**
   * Constructs the main menu.
   *
   * @param connectionManager the connection manager
   * @param session the account session
   * @param notificationListener the notification listener
   * @param notifier the game end notifier
   * @param output the output stream
   * @param input the input reader
   * @param udpPort the UDP port for notifications
   */
  public MainMenu(
      ConnectionManager connectionManager,
      AccountSession session,
      NotificationListener notificationListener,
      GameEndNotifier notifier,
      PrintStream output,
      BufferedReader input,
      int udpPort) {
    this.connectionManager = connectionManager;
    this.session = session;
    this.notificationListener = notificationListener;
    this.notifier = notifier;
    this.output = output;
    this.input = input;
    this.udpPort = udpPort;
  }

  /**
   * Runs the menu loop until the user exits.
   *
   * @throws IOException if an I/O error occurs
   */
  public void run() throws IOException {
    while (true) {
      TerminalScreen.clear();
      TerminalScreen.printTitle();
      output.println();
      output.println("1. Login");
      output.println("2. Register");
      output.println("3. Update Credentials");
      output.println("4. Help");
      output.println("5. Exit");
      output.print("Choose an option: ");
      output.flush();

      String line = input.readLine();
      if (line == null) return;
      String choice = line.trim();

      switch (choice) {
        case "1" -> login();
        case "2" -> register();
        case "3" -> updateCredentials();
        case "4" -> showHelp();
        case "5" -> {
          return;
        }
        default -> output.println("Invalid option.");
      }
    }
  }

  /**
   * Performs the login flow and, if successful, starts the logged-in menu.
   *
   * @throws IOException if an I/O error occurs
   */
  private void login() throws IOException {
    output.print("Username: ");
    output.flush();
    String username = input.readLine();
    output.print("Password: ");
    output.flush();
    String password = input.readLine();

    ClientActions.LoginResult result =
        ClientActions.login(connectionManager, username, password, udpPort);
    TerminalScreen.clear();
    if (!result.success()) {
      output.println("Login failed: " + result.errorMessage());
      pressEnterToContinue();
      return;
    }

    session.setAccountToken(result.accountToken());
    notificationListener.start(udpPort, notification -> notifier.signal(notification.gameId()));

    output.println("Logged in as " + username);
    output.println();
    LoggedInMenu loggedInMenu =
        new LoggedInMenu(connectionManager, session, notifier, output, input);
    loggedInMenu.run();

    notificationListener.stop();
    session.clear();
  }

  /**
   * Performs the registration flow.
   *
   * @throws IOException if an I/O error occurs
   */
  private void register() throws IOException {
    output.print("Username: ");
    output.flush();
    String username = input.readLine();
    output.print("Password: ");
    output.flush();
    String password = input.readLine();

    ClientActions.RegisterResult result =
        ClientActions.register(connectionManager, username, password);
    TerminalScreen.clear();
    if (result.success()) {
      output.println("Registered successfully: " + result.username());
    } else {
      output.println("Registration failed: " + result.errorMessage());
    }
    pressEnterToContinue();
  }

  /**
   * Performs the credential update flow for unauthenticated users.
   *
   * @throws IOException if an I/O error occurs
   */
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

    ClientActions.UpdateCredentialsResult result =
        ClientActions.updateCredentials(
            connectionManager, oldUsername, newUsername, oldPassword, newPassword);
    TerminalScreen.clear();
    if (result.success()) {
      output.println("Credentials updated successfully.");
    } else {
      output.println("Update failed: " + result.errorMessage());
    }
    pressEnterToContinue();
  }

  /**
   * Shows help information for the main menu.
   *
   * @throws IOException if an I/O error occurs
   */
  private void showHelp() throws IOException {
    TerminalScreen.clear();
    output.println("Main Menu Help:");
    output.println("  - Login: enter your credentials to start playing.");
    output.println("  - Register: create a new account.");
    output.println("  - Update Credentials: change your username and/or password.");
    output.println("  - Help: show this help.");
    output.println("  - Exit: close the client.");
    pressEnterToContinue();
  }

  /**
   * Prompts the user to press Enter to continue.
   *
   * @throws IOException if an I/O error occurs
   */
  private void pressEnterToContinue() throws IOException {
    output.println();
    output.print("Press Enter to continue...");
    output.flush();
    input.readLine();
  }
}
