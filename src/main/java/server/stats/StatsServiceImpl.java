package server.stats;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import server.account.AccountService;
import server.account.exceptions.InvalidTokenException;
import server.dto.AccountPrincipal;
import server.dto.GameWordGroups;
import server.dto.PlayerGame;
import server.game.GameClock;
import server.game.GameLogic;
import server.game.GameRepository;
import server.game.PlayerGameRepository;
import server.game.exceptions.GameNotFoundException;
import shared.dto.GameStatsData;
import shared.dto.MistakeHistogram;
import shared.dto.PlayerStatsData;

/**
 * Implementation of {@link StatsService} that computes game and player statistics using the game
 * clock and repositories.
 */
public record StatsServiceImpl(
    AccountService accountService,
    PlayerGameRepository playerGameRepository,
    GameRepository gameRepository,
    GameClock gameClock)
    implements StatsService {

  /** {@inheritDoc} */
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
    List<Set<String>> correctGroups = GameLogic.correctGroupsAsSets(gameWordGroups);
    long now = System.currentTimeMillis();
    boolean gameCompleted = gameClock.isCompleted(resolvedGameId, now);
    long expiresAt = gameClock.expiresAt(resolvedGameId);

    int totalParticipants = entries.size();
    List<ScoreCalculator.Outcome> outcomes =
        entries.stream().map(pg -> ScoreCalculator.outcome(pg, correctGroups)).toList();

    int winners =
        (int) outcomes.stream().filter(outcome -> outcome == ScoreCalculator.Outcome.WON).count();
    int completedPlayers =
        (int)
            outcomes.stream()
                .filter(
                    outcome ->
                        outcome == ScoreCalculator.Outcome.WON
                            || outcome == ScoreCalculator.Outcome.LOST)
                .count();
    int activePlayers =
        (int)
            outcomes.stream()
                .filter(outcome -> outcome == ScoreCalculator.Outcome.INCOMPLETE && !gameCompleted)
                .count();

    double totalScore =
        entries.stream().mapToDouble(pg -> ScoreCalculator.score(pg, correctGroups)).sum();
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

  /**
   * Represents a player's performance in a single game.
   *
   * @param gameId the game ID
   * @param outcome the game outcome
   * @param mistakes the number of wrong guesses
   */
  record Performance(long gameId, ScoreCalculator.Outcome outcome, int mistakes) {}

  /** {@inheritDoc} */
  @Override
  public PlayerStatsData getPlayerStats(String accountToken) throws InvalidTokenException {
    AccountPrincipal principal = accountService.resolve(accountToken);
    String username = principal.username();
    long now = System.currentTimeMillis();
    long currentGameId = gameClock.currentGameId(now);

    List<Performance> allPerformances =
        playerGameRepository.findPlayerGameByUsername(username).stream()
            .sorted(Comparator.comparingLong(PlayerGame::gameId))
            .flatMap(pg -> toPerformance(pg).stream())
            .filter(
                p ->
                    !(p.gameId() == currentGameId
                        && p.outcome() == ScoreCalculator.Outcome.INCOMPLETE
                        && !gameClock.isCompleted(p.gameId(), now)))
            .toList();

    int puzzlesCompleted = allPerformances.size();
    long wins =
        allPerformances.stream().filter(p -> p.outcome() == ScoreCalculator.Outcome.WON).count();
    long losses =
        allPerformances.stream().filter(p -> p.outcome() == ScoreCalculator.Outcome.LOST).count();
    long notFinished =
        allPerformances.stream()
            .filter(p -> p.outcome() == ScoreCalculator.Outcome.INCOMPLETE)
            .count();
    long perfectPuzzles =
        allPerformances.stream()
            .filter(p -> p.outcome() == ScoreCalculator.Outcome.WON && p.mistakes() == 0)
            .count();

    Map<Integer, Long> mistakeCounts =
        allPerformances.stream()
            .filter(p -> p.outcome() == ScoreCalculator.Outcome.WON)
            .collect(Collectors.groupingBy(Performance::mistakes, Collectors.counting()));

    Map<Integer, Integer> wonByMistakes =
        IntStream.rangeClosed(0, 3)
            .boxed()
            .collect(Collectors.toMap(i -> i, i -> mistakeCounts.getOrDefault(i, 0L).intValue()));

    MistakeHistogram mistakeHistogram =
        new MistakeHistogram(wonByMistakes, (int) losses, (int) notFinished);

    double winRate = puzzlesCompleted == 0 ? 0.0 : wins / (double) puzzlesCompleted;
    double lossRate = puzzlesCompleted == 0 ? 0.0 : losses / (double) puzzlesCompleted;

    record StreakAccumulator(int current, int max) {
      StreakAccumulator next(boolean won) {
        if (won) {
          int newCurrent = current + 1;
          return new StreakAccumulator(newCurrent, Math.max(max, newCurrent));
        } else {
          return new StreakAccumulator(0, max);
        }
      }
    }

    StreakAccumulator acc =
        allPerformances.stream()
            .reduce(
                new StreakAccumulator(0, 0),
                (a, p) -> a.next(p.outcome() == ScoreCalculator.Outcome.WON),
                (a, b) ->
                    new StreakAccumulator(
                        Math.max(a.current(), b.current()), Math.max(a.max(), b.max())));

    int maxStreak = acc.max();
    int currentStreak =
        (int)
            IntStream.iterate(allPerformances.size() - 1, i -> i >= 0, i -> i - 1)
                .takeWhile(i -> allPerformances.get(i).outcome() == ScoreCalculator.Outcome.WON)
                .count();

    return new PlayerStatsData(
        puzzlesCompleted,
        winRate,
        lossRate,
        currentStreak,
        maxStreak,
        (int) perfectPuzzles,
        mistakeHistogram);
  }

  /**
   * Converts a {@link PlayerGame} record to a {@link Performance} if the game exists.
   *
   * @param pg the player game record
   * @return an Optional containing the performance, or empty if game not found
   */
  private Optional<Performance> toPerformance(PlayerGame pg) {
    long gameId = pg.gameId();
    if (!gameRepository.exists(gameId)) {
      return Optional.empty();
    }
    GameWordGroups gameWordGroups = gameRepository.loadById(gameId);
    List<Set<String>> correctGroups = GameLogic.correctGroupsAsSets(gameWordGroups);
    ScoreCalculator.Outcome outcome = ScoreCalculator.outcome(pg, correctGroups);
    ScoreCalculator.CorrectWrongCount counts = ScoreCalculator.countCorrectWrong(pg, correctGroups);
    return Optional.of(new Performance(gameId, outcome, counts.wrong()));
  }
}
