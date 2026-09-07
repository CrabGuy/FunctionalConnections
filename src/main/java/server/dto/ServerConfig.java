package server.dto;

/**
 * Configuration parameters for the server.
 *
 * @param tcpPort the TCP port to listen on
 * @param gameDurationMillis the duration of each game in milliseconds
 * @param storageDirectory the directory for persistence files
 * @param persistenceIntervalMillis the interval between periodic snapshots in milliseconds
 * @param jwtSecret the secret key for JWT signing
 * @param tokenExpiryMillis the token expiry time in milliseconds
 * @param threadPoolSize the size of the worker thread pool
 * @param gameDataFile the file containing game definitions
 */
public record ServerConfig(
    int tcpPort,
    long gameDurationMillis,
    String storageDirectory,
    long persistenceIntervalMillis,
    String jwtSecret,
    long tokenExpiryMillis,
    int threadPoolSize,
    String gameDataFile) {}
