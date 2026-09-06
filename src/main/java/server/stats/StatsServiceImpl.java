package server.stats;

import java.util.*;
import java.util.stream.Collectors;
import server.account.AccountService;
import server.account.exceptions.InvalidTokenException;
import server.dto.AccountPrincipal;
import server.dto.GameWordGroups;
import server.dto.PlayerGame;
import server.game.GameClock;
import server.game.GameRepository;
import server.game.PlayerGameRepository;
import server.game.exceptions.GameNotFoundException;
import shared.dto.GameStatsData;
import shared.dto.PlayerStatsData;

public record StatsServiceImpl(
    AccountService accountService,
    PlayerGameRepository playerGameRepository,
    GameRepository gameRepository,
    GameClock gameClock)
    implements StatsService {

  @Override
  public GameStatsData getGameStats(String accountToken, Long gameId)
      throws GameNotFoundException, InvalidTokenException {
    accountService.resolve(accountToken);

    long resolvedGameId =
        (gameId == null) ? gameClock.currentGameId(System.currentTimeMillis()) : gameId;

    if (!gameRepository.exists(resolvedGameId)) {
      throw new GameNotFoundException(resolvedGameId);
    }

    List<PlayerGame> entries = playerGameRepository.findByGame(resolvedGameId);
    GameWordGroups gameWordGroups = gameRepository.loadById(resolvedGameId);

    List<Set<String>> correctGroups =
        gameWordGroups.groups().stream()
            .map(group -> Set.copyOf(group.words()))
            .collect(Collectors.toList());

    long now = System.currentTimeMillis();
    boolean gameCompleted = gameClock.isCompleted(resolvedGameId, now);
    long expiresAt = gameClock.expiresAt(resolvedGameId);

    int totalParticipants = entries.size();
    int activePlayers = 0;
    int completedPlayers = 0;
    int winners = 0;
    double totalScore = 0.0;

    for (PlayerGame pg : entries) {
      ScoreCalculator.Outcome outcome = ScoreCalculator.outcome(pg, correctGroups);

      totalScore += ScoreCalculator.score(pg, correctGroups);

      switch (outcome) {
        case WON -> {
          winners++;
          completedPlayers++;
        }
        case LOST -> completedPlayers++;
        case INCOMPLETE -> {
          if (!gameCompleted) {
            activePlayers++;
          }
        }
      }
    }

    double averageScore = totalParticipants == 0 ? 0.0 : totalScore / totalParticipants;

    return new GameStatsData(
        resolvedGameId,
        gameCompleted,
        expiresAt,
        totalParticipants,
        activePlayers,
        completedPlayers,
        winners,
        averageScore);
  }

  @Override
  public PlayerStatsData getPlayerStats(String accountToken) throws InvalidTokenException {
    AccountPrincipal principal = accountService.resolve(accountToken);
    String username = principal.username();

    List<PlayerGame> games = playerGameRepository.findPlayerGameByUsername(username);
    // Sort by game id (chronological order)
    games.sort(Comparator.comparingLong(PlayerGame::gameId));

    int puzzlesCompleted = 0;
    int wins = 0;
    int losses = 0;
    int perfectPuzzles = 0;

    Map<Integer, Integer> mistakeHistogram = new HashMap<>();
    for (int i = 0; i <= 3; i++) {
      mistakeHistogram.put(i, 0);
    }

    List<Boolean> completedOutcomes = new ArrayList<>();

    for (PlayerGame pg : games) {
      long gameId = pg.gameId();
      if (!gameRepository.exists(gameId)) {
        continue;
      }
      GameWordGroups gameWordGroups = gameRepository.loadById(gameId);
      List<Set<String>> correctGroups =
          gameWordGroups.groups().stream()
              .map(group -> Set.copyOf(group.words()))
              .collect(Collectors.toList());

      ScoreCalculator.Outcome outcome = ScoreCalculator.outcome(pg, correctGroups);

      if (outcome == ScoreCalculator.Outcome.INCOMPLETE) {
        continue; // skip incomplete games for completed stats
      }

      puzzlesCompleted++;
      ScoreCalculator.CorrectWrongCount counts =
          ScoreCalculator.countCorrectWrong(pg, correctGroups);

      if (outcome == ScoreCalculator.Outcome.WON) {
        wins++;
        if (counts.wrong() == 0) {
          perfectPuzzles++;
        }
        mistakeHistogram.merge(counts.wrong(), 1, Integer::sum);
        completedOutcomes.add(true);
      } else { // LOST
        losses++;
        completedOutcomes.add(false);
      }
    }

    double winRate = puzzlesCompleted == 0 ? 0.0 : ((double) wins / puzzlesCompleted) * 100.0;
    double lossRate = puzzlesCompleted == 0 ? 0.0 : ((double) losses / puzzlesCompleted) * 100.0;

    int currentStreak = 0;
    int maxStreak = 0;
    int tempStreak = 0;
    for (boolean won : completedOutcomes) {
      if (won) {
        tempStreak++;
        maxStreak = Math.max(maxStreak, tempStreak);
      } else {
        tempStreak = 0;
      }
    }
    // current streak: iterate from most recent backwards
    for (int i = completedOutcomes.size() - 1; i >= 0; i--) {
      if (completedOutcomes.get(i)) {
        currentStreak++;
      } else {
        break;
      }
    }

    return new PlayerStatsData(
        puzzlesCompleted,
        winRate,
        lossRate,
        currentStreak,
        maxStreak,
        perfectPuzzles,
        Map.copyOf(mistakeHistogram));
  }
}
