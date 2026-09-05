package server.stats;

import server.account.AccountService;
import server.account.exceptions.InvalidTokenException;
import server.dto.GameWordGroups;
import server.dto.PlayerGame;
import server.game.GameRepository;
import server.game.PlayerGameRepository;
import shared.dto.LeaderboardData;
import shared.dto.LeaderboardEntry;

import java.util.*;
import java.util.stream.Collectors;

public record LeaderboardServiceImpl(
        AccountService accountService,
        PlayerGameRepository playerGameRepository,
        GameRepository gameRepository
) implements LeaderboardService {

    @Override
    public LeaderboardData getLeaderboard(String accountToken, String playerName, Integer topK)
            throws InvalidTokenException {
        accountService.resolve(accountToken);

        Set<String> allUsernames = playerGameRepository.findAllUsernames();
        List<LeaderboardEntry> allEntries = new ArrayList<>();

        for (String username : allUsernames) {
            List<PlayerGame> games = playerGameRepository.findPlayerGameByUsername(username);
            int totalScore = 0;

            for (PlayerGame pg : games) {
                long gameId = pg.gameId();
                if (!gameRepository.exists(gameId)) {
                    continue;
                }
                GameWordGroups gameWordGroups = gameRepository.loadById(gameId);
                List<Set<String>> correctGroups = gameWordGroups.groups().stream()
                        .map(group -> Set.copyOf(group.words()))
                        .collect(Collectors.toList());

                totalScore += ScoreCalculator.score(pg, correctGroups);
            }

            allEntries.add(new LeaderboardEntry(username, totalScore, 0)); // rank filled later
        }

        // Sort by score descending, then username ascending
        allEntries.sort(Comparator
                .comparingInt(LeaderboardEntry::score).reversed()
                .thenComparing(LeaderboardEntry::username));

        // Assign ranks
        List<LeaderboardEntry> rankedEntries = new ArrayList<>();
        for (int i = 0; i < allEntries.size(); i++) {
            LeaderboardEntry e = allEntries.get(i);
            rankedEntries.add(new LeaderboardEntry(e.username(), e.score(), i + 1));
        }

        LeaderboardEntry requested = null;
        if (playerName != null) {
            for (LeaderboardEntry e : rankedEntries) {
                if (e.username().equals(playerName)) {
                    requested = e;
                    break;
                }
            }
        }


        List<LeaderboardEntry> top;
        if (topK == null) {
            top = rankedEntries;
        } else if (topK <= 0) {
            top = List.of();
        } else if (topK < rankedEntries.size()) {
            top = rankedEntries.subList(0, topK);
        } else {
            top = rankedEntries;
        }

        return new LeaderboardData(
                List.copyOf(top),
                requested,
                rankedEntries.size()
        );
    }
}