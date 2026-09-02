package server.account;

import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * Maps logged-in usernames to the UDP address they should receive
 * asynchronous notifications (e.g. game-end) on. Populated by
 * {@link AccountService#login} and cleared by {@link AccountService#logout};
 * read by Slice E's {@code NotificationService}. Implementations must be
 * thread-safe.
 */
public interface NotificationRegistry {

    void register(String username, InetSocketAddress udpAddress);

    void unregister(String username);

    Optional<InetSocketAddress> lookup(String username);
}