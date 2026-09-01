package server;

import config.ServerConfig;
import server.game.*;
import server.service.*;
import server.storage.MapStorage;
import server.storage.ServerSnapshot;
import server.storage.SnapshotSaver;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class ServerMain {
    private final ServerConfig config;
    private final UserManager userManager;
    private final GameRepository gameRepository;
    private final PlayerProgressStore progressStore;
    private final UdpRegistry udpRegistry;
    private final AuthService authService;
    private final GameService gameService;
    private final StatsService statsService;
    private final RequestDispatcher dispatcher;
    private final SnapshotSaver snapshotSaver;
    private final SystemGameClock clock;
    private final AtomicLong lastNotifiedGameId = new AtomicLong(-1);

    public ServerMain() throws Exception {
        this.config = ServerConfig.load();
        this.userManager = new UserManager();
        PuzzleBank puzzleBank = new PuzzleBank(config.puzzleFilePath());
        this.clock = new SystemGameClock(config.gameDuration());
        this.progressStore = new PlayerProgressStore();
        this.gameRepository = new GameRepository(puzzleBank, clock, progressStore,
                config.maxMistakes(), config.gameDuration());
        this.udpRegistry = new UdpRegistry();

        loadPersistedData();

        this.gameService = new GameService(gameRepository, clock);
        this.authService = new AuthService(userManager, gameService);

        PlayerStatsCalculator statsCalculator = new PlayerStatsCalculator(progressStore, gameRepository, clock);
        this.statsService = new StatsService(userManager, statsCalculator);

        this.dispatcher = new RequestDispatcher(authService, gameService, statsService);
        this.snapshotSaver = new SnapshotSaver(userManager, progressStore,
                config.storageDir(), config.saveIntervalSeconds());
        snapshotSaver.saveAll();
    }

    private void loadPersistedData() {
        try {
            Files.createDirectories(config.storageDir());
            Path snapshotPath = config.storageDir().resolve("snapshot.json");
            if (Files.exists(snapshotPath)) {
                ServerSnapshot snapshot = MapStorage.load(snapshotPath, ServerSnapshot.class);
                if (snapshot != null) {
                    if (snapshot.users() != null) {
                        userManager.loadSnapshot(snapshot.users());
                    }
                    if (snapshot.playerProgress() != null) {
                        progressStore.loadSnapshot(snapshot.playerProgress());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load persisted data: " + e.getMessage());
        }
    }

    public void start() {
        startUdpNotifier();
        Thread.ofPlatform().start(() -> {
            try (var server = new ServerSocket(config.tcpPort());
                 var executor = Executors.newFixedThreadPool(config.threadPoolSize())) {
                System.out.println("Server listening on port " + config.tcpPort());
                while (!Thread.currentThread().isInterrupted()) {
                    var socket = server.accept();
                    executor.submit(new ClientSessionHandler(socket, dispatcher, udpRegistry));
                }
            } catch (Exception e) {
                System.err.println("Server error: " + e.getMessage());
            }
        });
    }

    private void startUdpNotifier() {
        Thread.ofPlatform().daemon().start(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                while (!Thread.currentThread().isInterrupted()) {
                    long currentId = clock.currentGameId();
                    if (lastNotifiedGameId.get() != currentId) {
                        lastNotifiedGameId.set(currentId);
                        byte[] data = ("GAME_UPDATE:" + currentId).getBytes(StandardCharsets.UTF_8);
                        for (InetSocketAddress target : udpRegistry.getEndpoints()) {
                            try {
                                DatagramPacket packet = new DatagramPacket(data, data.length, target);
                                socket.send(packet);
                            } catch (Exception e) {
                                // Ignore individual transmission failures
                            }
                        }
                    }
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                System.err.println("UDP notifier error: " + e.getMessage());
            }
        });
    }

    public static void main(String[] args) throws Exception {
        new ServerMain().start();
    }
}