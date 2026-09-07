package server.account;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.Set;

/** Registry for mapping usernames to UDP addresses for game end notifications. */
public interface NotificationRegistry {

  /**
   * Registers or updates the UDP address for a username.
   *
   * @param username the username
   * @param udpAddress the UDP socket address
   */
  void register(String username, InetSocketAddress udpAddress);

  /**
   * Removes the registration for a username.
   *
   * @param username the username
   */
  void unregister(String username);

  /**
   * Looks up the UDP address for a username.
   *
   * @param username the username
   * @return an Optional containing the address if found
   */
  Optional<InetSocketAddress> lookup(String username);

  /**
   * Returns all currently registered usernames.
   *
   * @return a set of usernames
   */
  Set<String> getRegisteredUsernames();
}
