package server.stats;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import server.account.AccountService;
import server.account.exceptions.InvalidTokenException;
import server.dto.GameWordGroups;
import server.dto.PlayerGame;
import server.game.GameLogic;
import server.game.GameRepository;
import server.game.PlayerGameRepository;
import server.stats.exceptions.PlayerNotFoundException;
import shared.dto.LeaderboardData;
import shared.dto.LeaderboardEntry;

/**
 * Implementation of {@link LeaderboardService} that computes total scores for all players across
 * all their games.
 */
public record LeaderboardServiceImpl(
    AccountService accountService,
    PlayerGameRepository playerGameRepository,
    GameRepository gameRepository)
    implements LeaderboardService {

  /** {@inheritDoc} */
  @Override
  public LeaderboardData getLeaderboard(String accountToken, String playerName, Integer topK)
      throws InvalidTokenException, PlayerNotFoundException {
    accountService.resolve(accountToken);

    Set<String> allUsernames = playerGameRepository.findAllUsernames();
    Map<String, Integer> scoresByUsername =
        allUsernames.stream()
            .collect(Collectors.toMap(username -> username, this::computeTotalScore));

    List<Map.Entry<String, Integer>> sortedEntries =
        scoresByUsername.entrySet().stream()
            .sorted(
                Map.Entry.<String, Integer>comparingByValue()
                    .reversed()
                    .thenComparing(Map.Entry.comparingByKey()))
            .toList();

    List<LeaderboardEntry> rankedEntries =
        IntStream.range(0, sortedEntries.size())
            .mapToObj(
                i ->
                    new LeaderboardEntry(
                        sortedEntries.get(i).getKey(), sortedEntries.get(i).getValue(), i + 1))
            .toList();

    LeaderboardEntry requestedEntry = null;
    if (playerName != null) {
      Optional<LeaderboardEntry> requested =
          rankedEntries.stream().filter(entry -> entry.username().equals(playerName)).findFirst();
      if (requested.isEmpty()) {
        throw new PlayerNotFoundException(playerName);
      }
      requestedEntry = requested.get();
    }

    List<LeaderboardEntry> top;
    if (topK == null || topK >= rankedEntries.size()) {
      top = rankedEntries;
    } else if (topK <= 0) {
      top = List.of();
    } else {
      top = List.copyOf(rankedEntries.subList(0, topK));
    }

    return new LeaderboardData(top, requestedEntry, rankedEntries.size());
  }

  /**
   * Computes the total score for a given username by summing scores across all games.
   *
   * @param username the username
   * @return the total score
   */
  private int computeTotalScore(String username) {
    return playerGameRepository.findPlayerGameByUsername(username).stream()
        .flatMap(pg -> tryLoadGameAndScore(pg).stream())
        .mapToInt(Integer::intValue)
        .sum();
  }

  /**
   * Attempts to load the game and compute the score for a player game record.
   *
   * @param pg the player game record
   * @return an Optional containing the score, or empty if game not found or error
   */
  private Optional<Integer> tryLoadGameAndScore(PlayerGame pg) {
    try {
      if (!gameRepository.exists(pg.gameId())) {
        return Optional.empty();
      }
      GameWordGroups game = gameRepository.loadById(pg.gameId());
      List<Set<String>> correctGroups = GameLogic.correctGroupsAsSets(game);
      return Optional.of(ScoreCalculator.score(pg, correctGroups));
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
