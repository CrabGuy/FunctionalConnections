package server.storage;

import server.UserManager;
import server.game.GameRepository;
import server.game.PlayerProgressStore;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class SnapshotSaver {
    private final UserManager userManager;
    private final GameRepository gameRepository;
    private final PlayerProgressStore progressStore;
    private final Path storageDir;
    private final ScheduledExecutorService executor;

    public SnapshotSaver(UserManager userManager, GameRepository gameRepository,
                         PlayerProgressStore progressStore, Path storageDir, int saveIntervalSeconds) {
        this.userManager = userManager;
        this.gameRepository = gameRepository;
        this.progressStore = progressStore;
        this.storageDir = storageDir;
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "snapshot-saver");
            thread.setDaemon(true);
            return thread;
        });
        startPeriodicSave(saveIntervalSeconds);
    }

    private void startPeriodicSave(int intervalSeconds) {
        executor.scheduleWithFixedDelay(this::saveAll, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    public void saveAll() {
        try {
            java.nio.file.Files.createDirectories(storageDir);
            Map<String, server.User> usersSnapshot = userManager.snapshot();
            Map<Long, server.game.GameSession> gamesSnapshot = gameRepository.snapshot();
            Map<Long, Map<String, server.game.PlayerProgress>> progressSnapshot = progressStore.snapshot();

            MapStorage.save(storageDir.resolve("users.json"), usersSnapshot);
            MapStorage.save(storageDir.resolve("games.json"), gamesSnapshot);
            MapStorage.save(storageDir.resolve("player_progress.json"), progressSnapshot);
        } catch (Exception e) {
            System.err.println("Snapshot save failed: " + e.getMessage());
        }
    }
}