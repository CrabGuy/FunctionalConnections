package server;

import server.account.*;
import server.dto.*;
import server.network.*;
import server.game.*;
import server.persistence.*;
import server.stats.*;
import com.google.gson.Gson;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main entry point for the Connections game server.
 * <p>
 * Configuration is read from a standard Java properties file named
 * {@code server.properties}. The following keys are supported (defaults shown):
 * <ul>
 *   <li>{@code server.port} – TCP listen port (default 8080)</li>
 *   <li>{@code connection.pool.size} – number of threads handling client connections (default 10)</li>
 *   <li>{@code game.duration.millis} – duration of each game in milliseconds (default 3600000)</li>
 *   <li>{@code jwt.secret} – secret key used to sign JWTs (mandatory)</li>
 *   <li>{@code token.expiry.millis} – JWT token expiry in milliseconds (default 86400000)</li>
 *   <li>{@code game.data.file} – path to JSON file containing game word groups (default "games.json")</li>
 *   <li>{@code storage.directory} – directory for persistence snapshots (default ".")</li>
 *   <li>{@code snapshot.interval.millis} – interval between automatic snapshots (default 60000)</li>
 *   <li>{@code transition.poll.interval.millis} – polling interval for game transition watcher (default 1000)</li>
 * </ul>
 */
public class ServerMain {

    private static final String CONFIG_FILE = "server.properties";
    private static final int DEFAULT_PORT = 8080;
    private static final int DEFAULT_POOL_SIZE = 10;
    private static final long DEFAULT_GAME_DURATION_MILLIS = 3_600_000L;
    private static final long DEFAULT_SNAPSHOT_INTERVAL_MILLIS = 60_000L;
    private static final long DEFAULT_TOKEN_EXPIRY_MILLIS = 86_400_000L;
    private static final long DEFAULT_TRANSITION_POLL_MILLIS = 1_000L;
    private static final String DEFAULT_GAME_DATA_FILE = "games.json";
    private static final String DEFAULT_STORAGE_DIRECTORY = ".";

    public static void main(String[] args) throws Exception {
        // 1. Load configuration
        Properties props = loadProperties(CONFIG_FILE);
        int port = parseInt(props.getProperty("server.port"), DEFAULT_PORT);
        int poolSize = parseInt(props.getProperty("connection.pool.size"), DEFAULT_POOL_SIZE);
        long gameDurationMillis = parseLong(props.getProperty("game.duration.millis"),
                DEFAULT_GAME_DURATION_MILLIS);
        String jwtSecret = props.getProperty("jwt.secret");
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new IllegalStateException("Missing required property 'jwt.secret' in " + CONFIG_FILE);
        }
        long tokenExpiryMillis = parseLong(props.getProperty("token.expiry.millis"),
                DEFAULT_TOKEN_EXPIRY_MILLIS);
        String gameDataFile = props.getProperty("game.data.file", DEFAULT_GAME_DATA_FILE);
        String storageDirectory = props.getProperty("storage.directory", DEFAULT_STORAGE_DIRECTORY);
        long snapshotIntervalMillis = parseLong(props.getProperty("snapshot.interval.millis"),
                DEFAULT_SNAPSHOT_INTERVAL_MILLIS);
        long transitionPollMillis = parseLong(props.getProperty("transition.poll.interval.millis"),
                DEFAULT_TRANSITION_POLL_MILLIS);

        ServerConfig config = new ServerConfig(
                port,
                gameDurationMillis,
                storageDirectory,
                snapshotIntervalMillis,
                jwtSecret,
                tokenExpiryMillis,
                poolSize,
                gameDataFile
        );

        // 2. Repositories
        AccountRepository accountRepo = new InMemoryAccountRepository();
        PlayerGameRepository playerGameRepo = new InMemoryPlayerGameRepository();
        GameRepository gameRepo = new FileGameRepository(gameDataFile);

        // 3. Helpers & shared objects
        PasswordHasher hasher = new Sha256PasswordHasher();
        TokenSigner tokenSigner = new JwtTokenSigner(jwtSecret);
        NotificationRegistry notificationRegistry = new InMemoryNotificationRegistry();
        Gson gson = new Gson();

        // 4. Services
        AccountService accountService = new AccountServiceImpl(
                accountRepo, hasher, tokenSigner, notificationRegistry, config);
        GameClock gameClock = new GameClockImpl(gameDurationMillis);

        ProposalService proposalService = new ProposalServiceImpl(
                accountService, gameRepo, gameClock, playerGameRepo);
        StatsService statsService = new StatsServiceImpl(
                accountService, playerGameRepo, gameRepo, gameClock);
        LeaderboardService leaderboardService = new LeaderboardServiceImpl(
                accountService, playerGameRepo, gameRepo);

        NotificationService notificationService = new NotificationServiceImpl(notificationRegistry, gson);

        InetSocketAddress serverAddress = new InetSocketAddress(port);
        RequestDispatcher dispatcher = new RequestDispatcherImpl(
                accountService, proposalService, statsService, leaderboardService,
                gameClock, serverAddress);

        // 5. Persistence
        Path storagePath = Path.of(storageDirectory);
        PersistenceService persistence = new FilePersistenceService(storagePath, snapshotIntervalMillis);
        try {
            persistence.loadSnapshot(accountRepo, playerGameRepo);
            System.out.println("Snapshot loaded from " + storageDirectory);
        } catch (IOException e) {
            System.err.println("No snapshot found or load failed; starting fresh. " + e.getMessage());
        }

        // 6. NIO server socket
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(serverAddress);
        serverSocketChannel.configureBlocking(false);
        Selector selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server listening on port " + port);

        // 7. Thread pool
        ExecutorService connectionPool = Executors.newFixedThreadPool(poolSize);

        // 8. Background tasks
        // Game transition watcher
        Thread transitionWatcher = new Thread(new GameTransitionWatcherImpl(
                gameClock, playerGameRepo, proposalService, notificationService,
                transitionPollMillis),
                "game-transition-watcher");
        transitionWatcher.setDaemon(true);
        transitionWatcher.start();

        // Periodic snapshot scheduler
        ScheduledExecutorService snapshotScheduler = Executors.newSingleThreadScheduledExecutor();
        snapshotScheduler.scheduleAtFixedRate(
                () -> {
                    try {
                        persistence.saveSnapshot(accountRepo, playerGameRepo);
                    } catch (IOException e) {
                        System.err.println("Snapshot save failed: " + e.getMessage());
                    }
                },
                snapshotIntervalMillis,
                snapshotIntervalMillis,
                TimeUnit.MILLISECONDS);

        // 9. Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            try {
                snapshotScheduler.shutdownNow();
                connectionPool.shutdownNow();
                serverSocketChannel.close();
                persistence.saveSnapshot(accountRepo, playerGameRepo);
            } catch (IOException e) {
                System.err.println("Final snapshot save failed: " + e.getMessage());
            }
        }));

        // 10. Accept loop
        while (true) {
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                keyIterator.remove();

                if (!key.isValid()) continue;

                if (key.isAcceptable()) {
                    SocketChannel clientChannel = serverSocketChannel.accept();
                    if (clientChannel != null) {
                        clientChannel.configureBlocking(true);
                        // Create handler, set its channel, and submit to pool
                        ConnectionHandlerImpl handler = new ConnectionHandlerImpl(dispatcher, gson);
                        handler.bind(clientChannel);
                        connectionPool.submit(handler);
                    }
                }
            }
        }
    }

    private static Properties loadProperties(String filename) throws IOException {
        Properties props = new Properties();
        try (InputStream input = new FileInputStream(filename)) {
            props.load(input);
        }
        return props;
    }

    private static int parseInt(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) return defaultValue;
        return Integer.parseInt(value.trim());
    }

    private static long parseLong(String value, long defaultValue) {
        if (value == null || value.trim().isEmpty()) return defaultValue;
        return Long.parseLong(value.trim());
    }
}