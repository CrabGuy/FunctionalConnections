package client.app;

import client.config.ClientConfig;
import client.config.ClientConfigLoader;
import client.connection.ConnectionManager;
import client.connection.NioConnectionManager;
import client.notification.NotificationListener;
import client.notification.UdpNotificationListener;
import client.session.AccountSession;
import client.ui.FullScreenClientCLI;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Entry point for the client application. Loads configuration, initializes connection, notification
 * and session components, and starts the full-screen CLI.
 */
public final class ClientMain {

  private ClientMain() {
    // Prevent instantiation
  }

  /**
   * Main method that starts the client.
   *
   * @param ignored command-line arguments (not used)
   */
  public static void main(String[] ignored) {
    try {
      ClientConfig config = ClientConfigLoader.load(Path.of("config/client.properties"));
      AccountSession session = new AccountSession();
      ConnectionManager connectionManager = new NioConnectionManager();
      NotificationListener notificationListener = new UdpNotificationListener();

      FullScreenClientCLI cli =
          new FullScreenClientCLI(config, session, connectionManager, notificationListener);
      cli.start();
    } catch (IOException exception) {
      System.err.println("Client startup failed: " + exception.getMessage());
      System.exit(1);
    }
  }
}
