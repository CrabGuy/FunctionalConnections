package server.network;

import java.util.*;
import java.util.stream.Collectors;
import server.account.NotificationRegistry;
import server.dto.GameWordGroups;
import server.dto.PlayerGame;
import server.dto.WordGroup;
import server.game.GameClock;
import server.game.GameRepository;
import server.game.PlayerGameRepository;
import server.game.ProposalService;
import shared.dto.GameInfoData;

public final class GameTransitionWatcherImpl implements GameTransitionWatcher {
  private final GameClock gameClock;
  private final PlayerGameRepository playerGameRepository;
  private final ProposalService proposalService;
  private final NotificationService notificationService;
  private final NotificationRegistry notificationRegistry;
  private final GameRepository gameRepository;
  private final long pollIntervalMillis;
  private volatile boolean shouldStop = false;
  private long lastObservedGameId = -1;

  public GameTransitionWatcherImpl(
      GameClock gameClock,
      PlayerGameRepository playerGameRepository,
      ProposalService proposalService,
      NotificationService notificationService,
      NotificationRegistry notificationRegistry,
      GameRepository gameRepository,
      long pollIntervalMillis) {
    this.gameClock = gameClock;
    this.playerGameRepository = playerGameRepository;
    this.proposalService = proposalService;
    this.notificationService = notificationService;
    this.notificationRegistry = notificationRegistry;
    this.gameRepository = gameRepository;
    this.pollIntervalMillis = pollIntervalMillis;
  }

  @Override
  public void run() {
    long now = System.currentTimeMillis();
    long currentId = gameClock.currentGameId(now);
    lastObservedGameId = currentId;
    while (!shouldStop && !Thread.currentThread().isInterrupted()) {
      try {
        Thread.sleep(pollIntervalMillis);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
      now = System.currentTimeMillis();
      currentId = gameClock.currentGameId(now);
      if (currentId != lastObservedGameId) {
        handleGameTransition(lastObservedGameId);
        lastObservedGameId = currentId;
      }
    }
  }

  public void stop() {
    shouldStop = true;
  }

  private void handleGameTransition(long endedGameId) {
    Map<String, GameInfoData> results = new HashMap<>();

    for (PlayerGame pg : playerGameRepository.findByGame(endedGameId)) {
      try {
        GameInfoData info = proposalService.getGameInfoForUsername(endedGameId, pg.username());
        results.put(pg.username(), info);
      } catch (Exception e) {
      }
    }

    GameWordGroups gameWordGroups = gameRepository.loadById(endedGameId);
    List<List<String>> correctGroups =
        gameWordGroups.groups().stream()
            .map(WordGroup::words)
            .collect(Collectors.toUnmodifiableList());

    for (String username : notificationRegistry.getRegisteredUsernames()) {
      if (!results.containsKey(username)) {
        results.put(username, buildEmptyGameInfo(endedGameId, correctGroups));
      }
    }

    notificationService.notifyGameEnd(results);
  }

  private GameInfoData buildEmptyGameInfo(long gameId, List<List<String>> correctGroups) {
    GameWordGroups game = gameRepository.loadById(gameId);
    List<String> words =
        game.groups().stream().flatMap(g -> g.words().stream()).collect(Collectors.toList());
    Collections.shuffle(words, new Random(gameId));
    return new GameInfoData(
        gameId,
        gameClock.expiresAt(gameId),
        List.copyOf(words),
        List.of(),
        List.of(),
        correctGroups);
  }
}
