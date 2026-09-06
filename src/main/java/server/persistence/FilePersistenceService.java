package server.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import server.account.AccountRepository;
import server.dto.Account;
import server.dto.PlayerGame;
import server.game.PlayerGameRepository;

/**
 * Persistence service that saves accounts and player games to JSON files. Uses Gson for
 * serialization/deserialization. Files are stored in the configured storage directory. The service
 * can be asked to periodically save snapshots using a background thread.
 */
public class FilePersistenceService implements PersistenceService {

  private final Path storageDirectory;
  private final long intervalMillis;
  private final Gson gson;
  private ScheduledExecutorService scheduler;

  /**
   * Full constructor with explicit persistence interval.
   *
   * @param storageDirectory the directory where snapshot files will be stored
   * @param intervalMillis the interval in milliseconds between automatic snapshots
   */
  public FilePersistenceService(Path storageDirectory, long intervalMillis) {
    this.storageDirectory = storageDirectory;
    this.intervalMillis = intervalMillis;
    this.gson =
        new GsonBuilder()
            .setPrettyPrinting()
            .create(); // record support is built-in since Gson 2.10
  }

  /**
   * Convenience constructor for tests or when periodic scheduling is not needed. Uses {@link
   * Long#MAX_VALUE} as the interval, effectively disabling automatic snapshots.
   *
   * @param storageDirectory the directory where snapshot files will be stored
   */
  public FilePersistenceService(Path storageDirectory) {
    this(storageDirectory, Long.MAX_VALUE);
  }

  @Override
  public void saveSnapshot(AccountRepository accounts, PlayerGameRepository playerGames)
      throws IOException {
    // Ensure storage directory exists
    Files.createDirectories(storageDirectory);

    // Save accounts
    List<Account> accountList = accounts.findAll();
    Path accountsFile = storageDirectory.resolve("accounts.json");
    String accountsJson = gson.toJson(accountList);
    Files.writeString(accountsFile, accountsJson);

    // Save player games
    List<PlayerGame> playerGameList = playerGames.findAll();
    Path playerGamesFile = storageDirectory.resolve("playerGames.json");
    String playerGamesJson = gson.toJson(playerGameList);
    Files.writeString(playerGamesFile, playerGamesJson);
  }

  @Override
  public void loadSnapshot(AccountRepository accounts, PlayerGameRepository playerGames)
      throws IOException {
    // Load accounts if file exists
    Path accountsFile = storageDirectory.resolve("accounts.json");
    if (Files.exists(accountsFile)) {
      String accountsJson = Files.readString(accountsFile);
      List<Account> accountList =
          gson.fromJson(accountsJson, new TypeToken<List<Account>>() {}.getType());
      accountList.forEach(accounts::save);
    }

    // Load player games if file exists
    Path playerGamesFile = storageDirectory.resolve("playerGames.json");
    if (Files.exists(playerGamesFile)) {
      String playerGamesJson = Files.readString(playerGamesFile);
      List<PlayerGame> playerGameList =
          gson.fromJson(playerGamesJson, new TypeToken<List<PlayerGame>>() {}.getType());
      playerGameList.forEach(playerGames::save);
    }
  }

  @Override
  public void schedulePeriodicSnapshot(
      AccountRepository accounts, PlayerGameRepository playerGames) {
    if (scheduler != null) {
      throw new IllegalStateException("Periodic snapshot already scheduled");
    }
    scheduler =
        Executors.newSingleThreadScheduledExecutor(
            runnable -> {
              Thread t = new Thread(runnable, "persistence-snapshot-thread");
              t.setDaemon(true);
              return t;
            });
    scheduler.scheduleAtFixedRate(
        () -> {
          try {
            saveSnapshot(accounts, playerGames);
          } catch (IOException e) {
            System.err.println("Failed to save snapshot: " + e.getMessage());
            e.printStackTrace();
          }
        },
        intervalMillis,
        intervalMillis,
        TimeUnit.MILLISECONDS);
  }

  /** Shuts down the periodic snapshot scheduler, if any. */
  public void shutdown() {
    if (scheduler != null) {
      scheduler.shutdown();
      scheduler = null;
    }
  }
}
