package client.command;

import client.config.ClientConfig;
import client.connection.ConnectionManager;
import client.notification.NotificationListener;
import client.session.AccountSession;
import java.io.PrintStream;

public record CommandContext(
    AccountSession session,
    ConnectionManager connectionManager,
    NotificationListener notificationListener,
    ClientConfig config,
    PrintStream output) {}
