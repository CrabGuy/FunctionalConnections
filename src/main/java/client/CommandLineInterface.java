package client;

import shared.dto.ApiRequest;
import java.io.IOException;


import client.dto.ClientConfig;

/**
 * Parses user commands from the command line, converts them into the
 * appropriate {@link ApiRequest} subtype, sends them via the
 * {@link ConnectionManager}, and prints the server's response to the console.
 *
 * <p>The implementation is responsible for the main interactive loop. It
 * receives commands as text lines, interprets them according to the protocol
 * specification, and displays the results in a human‑readable form. All
 * dependencies (connection manager, notification listener, configuration, and
 * game state) are injected via the {@link #start} method.</p>
 */
public interface CommandLineInterface {

    /**
     * Starts the command‑line interface and enters the main user input loop.
     *
     * <p>This method blocks until the user issues a command that terminates the
     * client (e.g., "quit" or "exit"). It uses the supplied
     * {@link ConnectionManager} to send requests, the
     * {@link NotificationListener} to start/stop asynchronous notifications,
     * the {@link ClientConfig} for any required runtime settings, and the
     * {@link ClientGameState} to maintain the current session.</p>
     *
     * @param config                client configuration read from file
     * @param connectionManager     persistent connection to the server
     * @param notificationListener  listener for UDP game‑end notifications
     * @param gameState             mutable client state (token, current game, etc.)
     * @throws IOException if an I/O error occurs while reading from standard input
     *                     or while communicating with the server
     */
    void start(ClientConfig config,
               ConnectionManager connectionManager,
               NotificationListener notificationListener,
               AccountSession accountSession) throws IOException;

    /**
     * Processes a single command string. This method is useful for testing or
     * for building a non‑interactive client shell. It performs the same
     * actions as the main loop would for the given input.
     *
     * @param command            the raw command line (e.g., "login alice secret")
     * @param connectionManager  the connection to use for sending requests
     * @param gameState          the current client state (may be updated)
     * @throws IOException if the command cannot be sent or the response cannot
     *                     be read
     */
    void processCommand(String command,
                        ConnectionManager connectionManager) throws IOException;
}