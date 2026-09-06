package client.config;

public record ClientConfig(String serverAddress, int tcpPort, int udpPort) {
}
