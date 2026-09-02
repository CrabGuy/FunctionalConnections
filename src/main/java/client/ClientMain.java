package client;

import client.dto.ClientConfig;

import java.io.IOException;
import java.nio.file.Path;

/** Application entry point for the command-line client. */
public final class ClientMain {
    private ClientMain() {
    }

    public static void main(String[] ignored) {
        try {
            ClientConfig config = ClientConfigLoader.load(Path.of("client.properties"));
            AccountSession session = new AccountSession();
            ConnectionManager connectionManager = new NioConnectionManager();
            NotificationListener notificationListener = new UdpNotificationListener();
            CommandLineInterface cli = new ClientCommandLineInterface(
                    config, session, connectionManager, notificationListener);
            cli.start();
        } catch (IOException exception) {
            System.err.println("Client startup failed: " + exception.getMessage());
            System.exit(1);
        }
    }
}
