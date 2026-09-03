package server.account;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe in-memory registry mapping usernames to their UDP notification addresses.
 */
public final class InMemoryNotificationRegistry implements NotificationRegistry {

    private final ConcurrentMap<String, InetSocketAddress> udpAddresses = new ConcurrentHashMap<>();

    @Override
    public void register(String username, InetSocketAddress udpAddress) {
        udpAddresses.put(username, udpAddress);
    }

    @Override
    public void unregister(String username) {
        udpAddresses.remove(username);
    }

    @Override
    public Optional<InetSocketAddress> lookup(String username) {
        return Optional.ofNullable(udpAddresses.get(username));
    }
}