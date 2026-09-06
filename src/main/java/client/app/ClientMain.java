package client.app;

import client.command.ClientCommandLineInterface;
import client.command.CommandLineInterface;
import client.config.ClientConfig;
import client.config.ClientConfigLoader;
import client.connection.ConnectionManager;
import client.connection.NioConnectionManager;
import client.notification.NotificationListener;
import client.notification.UdpNotificationListener;
import client.session.AccountSession;
import java.io.IOException;
import java.nio.file.Path;

public final class ClientMain {
  private ClientMain() {}

  public static void main(String[] ignored) {
    try {
      ClientConfig config = ClientConfigLoader.load(Path.of("config/client.properties"));
      AccountSession session = new AccountSession();
      ConnectionManager connectionManager = new NioConnectionManager();
      NotificationListener notificationListener = new UdpNotificationListener();
      CommandLineInterface cli =
          new ClientCommandLineInterface(config, session, connectionManager, notificationListener);
      cli.start();
    } catch (IOException exception) {
      System.err.println("Client startup failed: " + exception.getMessage());
      System.exit(1);
    }
  }
}
