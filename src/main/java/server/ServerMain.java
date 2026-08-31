package server;

import config.ServerConfig;
import server.game.*;
import server.service.*;
import server.storage.MapStorage;
import server.storage.SnapshotSaver;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class ServerMain {
    private final ServerConfig config;
    private final UserManager userManager;
    private final GameRepository gameRepository;
    private final AuthService authService;
    private final GameService gameService;
    private final StatsService statsService;
    private final RequestDispatcher dispatcher;
    private final SnapshotSaver snapshotSaver;
    private final AtomicLong lastNotifiedGameId = new AtomicLong(-1);

    public ServerMain() throws Exception {
        this.config = ServerConfig.load();
        this.userManager = new UserManager();
        PuzzleBank puzzleBank = new PuzzleBank(config.puzzleFilePath());
        SystemGameClock clock = new SystemGameClock(config.gameDuration());
        this.gameRepository = new GameRepository(puzzleBank, clock, config.maxInMemoryGames(),
                config.storageDir(), config.maxMistakes(), config.gameDuration());
        loadPersistedData();
        this.authService = new AuthService(userManager, gameRepository, clock);
        this.gameService = new GameService(gameRepository, clock);
        this.statsService = new StatsService(userManager);
        this.dispatcher = new RequestDispatcher(authService, gameService, statsService);
        this.snapshotSaver = new SnapshotSaver(userManager, gameRepository, gameRepository.progressStore(),
                config.storageDir(), config.saveIntervalSeconds());
        snapshotSaver.saveAll();
    }

    private void loadPersistedData() {
        try {
            Files.createDirectories(config.storageDir());
            Path usersPath = config.storageDir().resolve("users.json");
            Path progressPath = config.storageDir().resolve("player_progress.json");

            if (Files.exists(usersPath)) {
                Map<String, User> usersSnapshot = MapStorage.load(usersPath, String.class, User.class);
                userManager.loadSnapshot(usersSnapshot);
            }
            if (Files.exists(progressPath)) {
                TypeFactory typeFactory = TypeFactory.defaultInstance();
                JavaType innerType = typeFactory.constructMapType(Map.class, String.class, PlayerProgress.class);
                JavaType outerType = typeFactory.constructMapType(Map.class,
                        typeFactory.constructType(Long.class), innerType);
                Map<Long, Map<String, PlayerProgress>> progressSnapshot = MapStorage.load(progressPath, outerType);
                gameRepository.progressStore().loadSnapshot(progressSnapshot);
            }
        } catch (Exception e) {
            System.err.println("Failed to load persisted data: " + e.getMessage());
        }
    }

    public void start() {
        startUdpNotifier();
        Thread.ofPlatform().start(() -> {
            try (var server = new ServerSocket(config.tcpPort());
                 var executor = Executors.newFixedThreadPool(10)) {
                System.out.println("Server listening on port " + config.tcpPort());
                while (!Thread.currentThread().isInterrupted()) {
                    var socket = server.accept();
                    executor.submit(new ClientSessionHandler(socket, dispatcher));
                }
            } catch (Exception e) {
                System.err.println("Server error: " + e.getMessage());
            }
        });
    }

    private void startUdpNotifier() {
        Thread.ofPlatform().daemon().start(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                while (!Thread.currentThread().isInterrupted()) {
                    long currentGameId = gameRepository.getActiveGame().id();
                    var activeGame = gameRepository.getActiveGame();
                    var now = java.time.Instant.now();
                    if (activeGame.remainingTime(now).isZero() && lastNotifiedGameId.get() != currentGameId + 1) {
                        long nextGameId = currentGameId + 1;
                        lastNotifiedGameId.set(nextGameId);
                        byte[] data = ("GAME_UPDATE:" + nextGameId).getBytes(StandardCharsets.UTF_8);
                        DatagramPacket packet = new DatagramPacket(
                                data, data.length, InetAddress.getByName("255.255.255.255"), config.udpPort());
                        socket.send(packet);
                    }
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                System.err.println("UDP Notifier error: " + e.getMessage());
            }
        });
    }

    public static void main(String[] args) throws Exception {
        ServerMain server = new ServerMain();
        server.start();
        Thread.currentThread().join();
    }
}