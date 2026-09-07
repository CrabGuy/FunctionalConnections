package server.account;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory implementation of {@link NotificationRegistry} that maps usernames to their UDP
 * addresses.
 */
public final class InMemoryNotificationRegistry implements NotificationRegistry {

  private final ConcurrentMap<String, InetSocketAddress> udpAddresses = new ConcurrentHashMap<>();

  /** {@inheritDoc} */
  @Override
  public void register(String username, InetSocketAddress udpAddress) {
    udpAddresses.put(username, udpAddress);
  }

  /** {@inheritDoc} */
  @Override
  public void unregister(String username) {
    udpAddresses.remove(username);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<InetSocketAddress> lookup(String username) {
    return Optional.ofNullable(udpAddresses.get(username));
  }

  /** {@inheritDoc} */
  @Override
  public Set<String> getRegisteredUsernames() {
    return Set.copyOf(udpAddresses.keySet());
  }
}
