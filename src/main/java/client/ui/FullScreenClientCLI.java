package client.ui;

import client.config.ClientConfig;
import client.connection.ConnectionManager;
import client.notification.NotificationListener;
import client.session.AccountSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

/** The main CLI orchestrator that connects to the server and runs the main menu. */
public final class FullScreenClientCLI {

  private final PrintStream output;
  private final BufferedReader input;
  private final ClientConfig config;
  private final AccountSession session;
  private final ConnectionManager connectionManager;
  private final NotificationListener notificationListener;

  /**
   * Constructs a new CLI instance.
   *
   * @param config the client configuration
   * @param session the account session
   * @param connectionManager the connection manager
   * @param notificationListener the notification listener
   */
  public FullScreenClientCLI(
      ClientConfig config,
      AccountSession session,
      ConnectionManager connectionManager,
      NotificationListener notificationListener) {
    this.output = System.out;
    this.input = new BufferedReader(new InputStreamReader(System.in));
    this.config = config;
    this.session = session;
    this.connectionManager = connectionManager;
    this.notificationListener = notificationListener;
  }

  /**
   * Connects to the server, runs the main menu, and cleans up resources.
   *
   * @throws IOException if an I/O error occurs
   */
  public void start() throws IOException {
    connectionManager.connect(config.serverAddress(), config.tcpPort());

    GameEndNotifier notifier = new GameEndNotifier();
    MainMenu mainMenu =
        new MainMenu(
            connectionManager,
            session,
            notificationListener,
            notifier,
            output,
            input,
            config.udpPort());
    mainMenu.run();

    notificationListener.stop();
    connectionManager.close();
  }
}
