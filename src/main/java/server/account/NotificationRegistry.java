package server.account;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.Set;
public interface NotificationRegistry {
    void register(String username, InetSocketAddress udpAddress);
    void unregister(String username);
    Optional<InetSocketAddress> lookup(String username);
    Set<String> getRegisteredUsernames();
}
