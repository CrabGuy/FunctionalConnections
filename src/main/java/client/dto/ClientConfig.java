package client.dto;

/** Runtime configuration loaded from the client properties file. */
public record ClientConfig(String serverAddress, int tcpPort, int udpPort) {
}
