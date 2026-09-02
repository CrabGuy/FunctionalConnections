package client;

import java.io.IOException;

import client.dto.ClientGameState;

/**
 * Listens for asynchronous notifications from the server over UDP and updates
 * the client's local game state accordingly.
 *
 * <p>An implementation runs its own thread. It binds a UDP socket to the port
 * advertised during login. Incoming datagrams are expected to be JSON‑encoded
 * game‑end notifications (see {@code GameStatsData} in the shared package).
 * Upon receiving such a notification, the listener updates the provided
 * {@link ClientGameState} instance.</p>
 */
public interface NotificationListener {

    /**
     * Starts the UDP listener on the given port and begins processing
     * incoming notifications.
     *
     * <p>This method is non‑blocking and should launch a new thread or task
     * to handle datagrams. The socket remains bound until {@link #stop()} is
     * called.</p>
     *
     * @param udpPort   the local UDP port to bind (must match the port sent to
     *                  the server during login)
     * @param gameState the mutable client game state to update when a
     *                  notification arrives
     * @throws IOException if the UDP socket cannot be created or bound
     */
    void start(int udpPort, ClientGameState gameState) throws IOException;

    /**
     * Stops the listener, closes the UDP socket, and terminates the background
     * thread.
     */
    void stop();
}