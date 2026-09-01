package client.dto;

/**
 * Client configuration loaded from a configuration file.
 *
 * @param serverAddress the hostname or IP address of the server.
 * @param tcpPort       the TCP port for command/response communication.
 */
public record ClientConfig(String serverAddress, int tcpPort) {
}
