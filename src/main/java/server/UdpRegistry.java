package server;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public final class UdpRegistry {
    private final ConcurrentHashMap<String, InetSocketAddress> endpoints = new ConcurrentHashMap<>();

    public void register(String username, InetSocketAddress address) {
        if (username != null && address != null) {
            endpoints.put(username, address);
        }
    }

    public Collection<InetSocketAddress> getEndpoints() {
        return endpoints.values();
    }
}