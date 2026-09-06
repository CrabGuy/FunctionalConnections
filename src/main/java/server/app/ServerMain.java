package server.app;

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
import server.account.*;
import server.dto.*;
import server.game.*;
import server.network.*;
import server.persistence.*;
import server.stats.*;

/**
 * Main entry point for the Connections game server. Uses non‑blocking I/O with a selector and a
 * worker thread pool.
 */
public class ServerMain {

  private static final String CONFIG_FILE = "config/server.properties";
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
    long gameDurationMillis =
        parseLong(props.getProperty("game.duration.millis"), DEFAULT_GAME_DURATION_MILLIS);
    String jwtSecret = props.getProperty("jwt.secret");
    if (jwtSecret == null || jwtSecret.isEmpty()) {
      throw new IllegalStateException("Missing required property 'jwt.secret' in " + CONFIG_FILE);
    }
    long tokenExpiryMillis =
        parseLong(props.getProperty("token.expiry.millis"), DEFAULT_TOKEN_EXPIRY_MILLIS);
    String gameDataFile = props.getProperty("game.data.file", DEFAULT_GAME_DATA_FILE);
    String storageDirectory = props.getProperty("storage.directory", DEFAULT_STORAGE_DIRECTORY);
    long snapshotIntervalMillis =
        parseLong(props.getProperty("snapshot.interval.millis"), DEFAULT_SNAPSHOT_INTERVAL_MILLIS);
    long transitionPollMillis =
        parseLong(
            props.getProperty("transition.poll.interval.millis"), DEFAULT_TRANSITION_POLL_MILLIS);

    ServerConfig config =
        new ServerConfig(
            port,
            gameDurationMillis,
            storageDirectory,
            snapshotIntervalMillis,
            jwtSecret,
            tokenExpiryMillis,
            poolSize,
            gameDataFile);

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
    AccountService accountService =
        new AccountServiceImpl(accountRepo, hasher, tokenSigner, notificationRegistry, config);
    GameClock gameClock = new GameClockImpl(gameDurationMillis);

    ProposalService proposalService =
        new ProposalServiceImpl(accountService, gameRepo, gameClock, playerGameRepo);
    StatsService statsService =
        new StatsServiceImpl(accountService, playerGameRepo, gameRepo, gameClock);
    LeaderboardService leaderboardService =
        new LeaderboardServiceImpl(accountService, playerGameRepo, gameRepo);

    NotificationService notificationService =
        new NotificationServiceImpl(notificationRegistry, gson);

    RequestDispatcher dispatcher =
        new RequestDispatcherImpl(
            accountService, proposalService, statsService, leaderboardService, gameClock);

    // 5. Persistence
    Path storagePath = Path.of(storageDirectory);
    PersistenceService persistence =
        new FilePersistenceService(storagePath, snapshotIntervalMillis);
    try {
      persistence.loadSnapshot(accountRepo, playerGameRepo);
      System.out.println("Snapshot loaded from " + storageDirectory);
    } catch (IOException e) {
      System.err.println("No snapshot found or load failed; starting fresh. " + e.getMessage());
    }

    // 6. NIO server socket
    InetSocketAddress serverAddress = new InetSocketAddress(port);
    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.bind(serverAddress);
    serverSocketChannel.configureBlocking(false);
    Selector selector = Selector.open();
    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    System.out.println("Server listening on port " + port);

    // 7. Worker thread pool (used only for request processing, not for blocking
    // I/O)
    ExecutorService workerPool = Executors.newFixedThreadPool(poolSize);

    // 8. Background tasks
    GameTransitionWatcher watcher =
        new GameTransitionWatcherImpl(
            gameClock,
            playerGameRepo,
            proposalService,
            notificationService,
            notificationRegistry,
            gameRepo,
            transitionPollMillis);
    Thread transitionWatcherThread = new Thread(watcher, "game-transition-watcher");
    transitionWatcherThread.setDaemon(true);
    transitionWatcherThread.start();

    persistence.schedulePeriodicSnapshot(accountRepo, playerGameRepo);

    // 9. Shutdown hook
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  System.out.println("Shutting down...");
                  try {
                    persistence.shutdown();
                    workerPool.shutdownNow();
                    serverSocketChannel.close();
                    persistence.saveSnapshot(accountRepo, playerGameRepo);
                  } catch (IOException e) {
                    System.err.println("Final snapshot save failed: " + e.getMessage());
                  }
                }));

    // 10. Selector loop (single thread for all I/O readiness)
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
            clientChannel.configureBlocking(false);
            ClientConnection conn = new ClientConnection(clientChannel, dispatcher, gson, selector);
            clientChannel.register(selector, SelectionKey.OP_READ, conn);
          }
        } else if (key.isReadable()) {
          // Read data and offload request processing to worker pool
          ClientConnection conn = (ClientConnection) key.attachment();
          conn.handleRead(workerPool);
        } else if (key.isWritable()) {
          // Write pending data
          ClientConnection conn = (ClientConnection) key.attachment();
          conn.handleWrite();
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
