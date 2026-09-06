package client;

import client.dto.ClientConfig;
import java.io.PrintStream;

public record CommandContext(
    AccountSession session,
    ConnectionManager connectionManager,
    NotificationListener notificationListener,
    ClientConfig config,
    PrintStream output
) {}
