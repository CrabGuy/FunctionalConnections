package server.dto;
public record ServerConfig(
        int tcpPort,
        long gameDurationMillis,
        String storageDirectory,
        long persistenceIntervalMillis,
        String jwtSecret,
        long tokenExpiryMillis,
        int threadPoolSize
) {}
