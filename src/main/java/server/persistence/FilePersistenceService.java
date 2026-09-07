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
 * Implementation of {@link PersistenceService} that saves and loads snapshots to JSON files in a
 * directory, with optional periodic scheduling.
 */
public class FilePersistenceService implements PersistenceService {

  private final Path storageDirectory;
  private final long intervalMillis;
  private final Gson gson;
  private ScheduledExecutorService scheduler;

  /**
   * Constructs the service with a custom snapshot interval.
   *
   * @param storageDirectory the directory to store snapshots
   * @param intervalMillis the interval for periodic snapshots
   */
  public FilePersistenceService(Path storageDirectory, long intervalMillis) {
    this.storageDirectory = storageDirectory;
    this.intervalMillis = intervalMillis;
    this.gson = new GsonBuilder().setPrettyPrinting().create();
  }

  /**
   * Constructs the service with no periodic scheduling (only manual snapshots).
   *
   * @param storageDirectory the directory to store snapshots
   */
  public FilePersistenceService(Path storageDirectory) {
    this(storageDirectory, Long.MAX_VALUE);
  }

  /** {@inheritDoc} */
  @Override
  public void saveSnapshot(AccountRepository accounts, PlayerGameRepository playerGames)
      throws IOException {
    Files.createDirectories(storageDirectory);

    List<Account> accountList = accounts.findAll();
    Path accountsFile = storageDirectory.resolve("accounts.json");
    String accountsJson = gson.toJson(accountList);
    Files.writeString(accountsFile, accountsJson);

    List<PlayerGame> playerGameList = playerGames.findAll();
    Path playerGamesFile = storageDirectory.resolve("playerGames.json");
    String playerGamesJson = gson.toJson(playerGameList);
    Files.writeString(playerGamesFile, playerGamesJson);
  }

  /** {@inheritDoc} */
  @Override
  public void loadSnapshot(AccountRepository accounts, PlayerGameRepository playerGames)
      throws IOException {
    Path accountsFile = storageDirectory.resolve("accounts.json");
    if (Files.exists(accountsFile)) {
      String accountsJson = Files.readString(accountsFile);
      List<Account> accountList =
          gson.fromJson(accountsJson, new TypeToken<List<Account>>() {}.getType());
      accountList.forEach(accounts::save);
    }

    Path playerGamesFile = storageDirectory.resolve("playerGames.json");
    if (Files.exists(playerGamesFile)) {
      String playerGamesJson = Files.readString(playerGamesFile);
      List<PlayerGame> playerGameList =
          gson.fromJson(playerGamesJson, new TypeToken<List<PlayerGame>>() {}.getType());
      playerGameList.forEach(playerGames::save);
    }
  }

  /** {@inheritDoc} */
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
