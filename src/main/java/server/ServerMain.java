package server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ServerMain {
    private static final int UDP_PORT = 9876;
    private static final Path STORAGE_DIR = Path.of("storage").toAbsolutePath().normalize();
    private static final long DEFAULT_SAVE_INTERVAL_SECONDS = 30;
    
    private final UserManager userManager;
    private final GameManager gameManager;
    private final RequestDispatcher dispatcher;
    private final AtomicLong lastNotifiedGameId = new AtomicLong(-1);
    private final long saveIntervalSeconds;

    public ServerMain() {
        this.userManager = new UserManager();
        this.gameManager = new GameManager("Connections_Data.json", Duration.ofMinutes(10), 4);
        this.dispatcher = new RequestDispatcher(gameManager, userManager);
        this.saveIntervalSeconds = Long.getLong("server.save.interval.seconds", DEFAULT_SAVE_INTERVAL_SECONDS);
        
        loadPersistedData();
        savePersistedData();
    }

    public void start(int port) {
        startUdpNotifier();
        startPeriodicSave();
        Thread.ofPlatform().start(() -> {
            try (var server = new ServerSocket(port);
                 var executor = Executors.newFixedThreadPool(10)) {
                System.out.println("Server listening on port " + port);
                while (!Thread.currentThread().isInterrupted()) {
                    var socket = server.accept();
                    executor.submit(new ClientSessionHandler(socket, dispatcher));
                }
            } catch (Exception e) {
                System.err.println("Server error: " + e.getMessage());
            }
        });
    }

    private void startPeriodicSave() {
        ScheduledExecutorService saveExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "storage-save");
            thread.setDaemon(true);
            return thread;
        });
        saveExecutor.scheduleWithFixedDelay(
                this::savePersistedData,
                saveIntervalSeconds,
                saveIntervalSeconds,
                TimeUnit.SECONDS
        );
    }

    private void loadPersistedData() {
        try {
            Files.createDirectories(STORAGE_DIR);
            userManager.load(STORAGE_DIR.resolve("users.json"));
            gameManager.load(STORAGE_DIR.resolve("games.json"));
            System.out.println("Loaded persisted data from " + STORAGE_DIR);
        } catch (Exception e) {
            System.err.println("Failed to load persisted data: " + e.getMessage());
        }
    }

    private void savePersistedData() {
        try {
            Files.createDirectories(STORAGE_DIR);
            userManager.save(STORAGE_DIR.resolve("users.json"));
            gameManager.save(STORAGE_DIR.resolve("games.json"));
        } catch (Exception e) {
            System.err.println("Failed to save persisted data: " + e.getMessage());
        }
    }

    private void startUdpNotifier() {
        Thread.ofPlatform().daemon().start(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                while (!Thread.currentThread().isInterrupted()) {
                    long currentGameId = gameManager.getCurrentGameId();
                    var activeGame = gameManager.getActiveGame();
                    if (activeGame.remainingTime().isZero()
                            && lastNotifiedGameId.get() != currentGameId + 1) {
                        long nextGameId = currentGameId + 1;
                        lastNotifiedGameId.set(nextGameId);
                        byte[] data = ("GAME_UPDATE:" + nextGameId).getBytes(StandardCharsets.UTF_8);
                        DatagramPacket packet = new DatagramPacket(
                                data,
                                data.length,
                                InetAddress.getByName("255.255.255.255"),
                                UDP_PORT
                        );
                        socket.send(packet);
                    }
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                System.err.println("UDP Notifier error: " + e.getMessage());
            }
        });
    }

    public static void main(String[] args) throws InterruptedException {
        ServerMain server = new ServerMain();
        server.start(8080);
        Thread.currentThread().join();
    }
}