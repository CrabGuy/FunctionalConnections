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
import shared.dto.LeaderboardData;
import shared.dto.LeaderboardEntry;

public record LeaderboardServiceImpl(
    AccountService accountService,
    PlayerGameRepository playerGameRepository,
    GameRepository gameRepository)
    implements LeaderboardService {

  @Override
  public LeaderboardData getLeaderboard(String accountToken, String playerName, Integer topK)
      throws InvalidTokenException {
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

    Optional<LeaderboardEntry> requested =
        Optional.ofNullable(playerName)
            .flatMap(
                name ->
                    rankedEntries.stream()
                        .filter(entry -> entry.username().equals(name))
                        .findFirst());

    List<LeaderboardEntry> top;
    if (topK == null || topK >= rankedEntries.size()) {
      top = rankedEntries;
    } else if (topK <= 0) {
      top = List.of();
    } else {
      top = List.copyOf(rankedEntries.subList(0, topK));
    }
    return new LeaderboardData(top, requested.orElse(null), rankedEntries.size());
  }

  private int computeTotalScore(String username) {
    return playerGameRepository.findPlayerGameByUsername(username).stream()
        .flatMap(pg -> tryLoadGameAndScore(pg).stream())
        .mapToInt(Integer::intValue)
        .sum();
  }

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
