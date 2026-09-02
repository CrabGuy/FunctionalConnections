package server.account;
import java.net.InetSocketAddress;
import java.util.Optional;
public interface NotificationRegistry {
    void register(String username, InetSocketAddress udpAddress);
    void unregister(String username);
    Optional<InetSocketAddress> lookup(String username);
}
