package server.game;

import server.account.AccountService;
import server.account.exceptions.InvalidTokenException;
import server.dto.AccountPrincipal;
import server.dto.GameWordGroups;
import server.dto.PlayerGame;
import server.dto.Proposal;
import server.dto.WordGroup;
import server.game.exceptions.*;
import shared.dto.GameInfoData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public record ProposalServiceImpl(
        AccountService accountService,
        GameRepository gameRepository,
        GameClock gameClock,
        PlayerGameRepository playerGameRepository
) implements ProposalService {

    @Override
    public GameInfoData submitProposal(String accountToken, long gameId, List<String> words)
            throws InvalidTokenException, InvalidProposalException,
                   GameNotCurrentException, PlayerAlreadyCompletedGameException {
        AccountPrincipal principal = accountService.resolve(accountToken);
        String username = principal.username();
        long currentGameId = gameClock.currentGameId(System.currentTimeMillis());
        if (gameId != currentGameId) {
            throw new GameNotCurrentException(gameId, currentGameId);
        }
        GameWordGroups game = gameRepository.loadById(gameId);
        PlayerGame playerGame = playerGameRepository.findOrCreate(username, gameId);

        GuessesSummary summary = summarize(playerGame, game.groups());
        if (isPlayerFinished(summary)) {
            throw new PlayerAlreadyCompletedGameException(username, gameId);
        }

        validateProposal(words, game, summary);

        Proposal newProposal = new Proposal(Set.copyOf(words));
        List<Proposal> updatedProposals = new ArrayList<>(playerGame.proposals());
        updatedProposals.add(newProposal);
        PlayerGame updatedPlayerGame = new PlayerGame(username, gameId, updatedProposals);
        playerGameRepository.save(updatedPlayerGame);

        GuessesSummary updatedSummary = summarize(updatedPlayerGame, game.groups());
        boolean includeCorrectGroups = isPlayerFinished(updatedSummary)
                || gameClock.isCompleted(gameId, System.currentTimeMillis());
        return buildGameInfoData(
                gameId,
                gameClock.expiresAt(gameId),
                shuffleWords(game, gameId),
                updatedPlayerGame,
                game.groups(),
                includeCorrectGroups,
                updatedSummary
        );
    }

    @Override
    public GameInfoData getGameInfo(String accountToken, Long gameId)
            throws InvalidTokenException, GameNotFoundException {
        AccountPrincipal principal = accountService.resolve(accountToken);
        String username = principal.username();
        long currentGameId = gameClock.currentGameId(System.currentTimeMillis());
        if (gameId != null && gameId > currentGameId) {
            throw new GameNotFoundException(gameId);
        }
        long effectiveGameId = (gameId == null)
                ? gameClock.currentGameId(System.currentTimeMillis())
                : gameId;
        if (!gameRepository.exists(effectiveGameId)) {
            throw new GameNotFoundException(effectiveGameId);
        }
        GameWordGroups game = gameRepository.loadById(effectiveGameId);
        PlayerGame playerGame = playerGameRepository.findOrCreate(username, effectiveGameId);
        GuessesSummary summary = summarize(playerGame, game.groups());
        boolean includeCorrectGroups = isPlayerFinished(summary)
                || gameClock.isCompleted(effectiveGameId, System.currentTimeMillis());
        return buildGameInfoData(
                effectiveGameId,
                gameClock.expiresAt(effectiveGameId),
                shuffleWords(game, effectiveGameId),
                playerGame,
                game.groups(),
                includeCorrectGroups,
                summary
        );
    }

    @Override
    public GameInfoData getGameInfoForUsername(long gameId, String username)
            throws GameNotFoundException {
        if (!gameRepository.exists(gameId)) {
            throw new GameNotFoundException(gameId);
        }
        PlayerGame playerGame = playerGameRepository.findByUsernameAndGame(username, gameId)
                .orElseThrow(() -> new IllegalStateException(
                        "Player " + username + " has no PlayerGame entry for game " + gameId));
        GameWordGroups game = gameRepository.loadById(gameId);
        GuessesSummary summary = summarize(playerGame, game.groups());
        boolean includeCorrectGroups = isPlayerFinished(summary)
                || gameClock.isCompleted(gameId, System.currentTimeMillis());
        return buildGameInfoData(
                gameId,
                gameClock.expiresAt(gameId),
                shuffleWords(game, gameId),
                playerGame,
                game.groups(),
                includeCorrectGroups,
                summary
        );
    }

    // ---------- Helper methods ----------

    private void validateProposal(List<String> words, GameWordGroups game, GuessesSummary summary)
            throws InvalidProposalException {
        if (words == null || words.size() != 4) {
            throw new MalformedProposalException();
        }
        if (new HashSet<>(words).size() != words.size()) {
            throw new MalformedProposalException();
        }
        Set<String> proposalSet = Set.copyOf(words);
        Set<String> allWords = getAllWords(game);
        if (!allWords.containsAll(proposalSet)) {
            throw new UnknownWordsInProposalException();
        }
        for (String word : proposalSet) {
            boolean alreadyGrouped = summary.correctGuesses().stream().anyMatch(set -> set.contains(word));
            if (alreadyGrouped) {
                throw new WordsAlreadyGroupedException();
            }
        }
    }

    private record GuessesSummary(List<Set<String>> correctGuesses, List<Set<String>> wrongGuesses) {}

    private static GuessesSummary summarize(PlayerGame playerGame, List<WordGroup> groups) {
        List<Set<String>> correct = new ArrayList<>();
        List<Set<String>> wrong = new ArrayList<>();
        for (Proposal proposal : playerGame.proposals()) {
            if (isCorrectProposal(proposal.words(), groups)) {
                correct.add(proposal.words());
            } else {
                wrong.add(proposal.words());
            }
        }
        return new GuessesSummary(correct, wrong);
    }

    private static boolean isPlayerFinished(GuessesSummary summary) {
        return summary.correctGuesses().size() >= 3 || summary.wrongGuesses().size() >= 4;
    }

    private static boolean isCorrectProposal(Set<String> proposalWords, List<WordGroup> groups) {
        for (WordGroup group : groups) {
            if (Set.copyOf(group.words()).equals(proposalWords)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> getAllWords(GameWordGroups game) {
        return game.groups().stream()
                .flatMap(g -> g.words().stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static List<String> shuffleWords(GameWordGroups game, long gameId) {
        List<String> allWords = game.groups().stream()
                .flatMap(g -> g.words().stream())
                .collect(Collectors.toList());
        Random random = new Random(gameId);
        List<String> shuffled = new ArrayList<>(allWords);
        java.util.Collections.shuffle(shuffled, random);
        return List.copyOf(shuffled);
    }

    private static GameInfoData buildGameInfoData(
            long gameId,
            long expiresAt,
            List<String> shuffledWords,
            PlayerGame playerGame,
            List<WordGroup> groups,
            boolean includeCorrectGroups,
            GuessesSummary summary
    ) {
        List<List<String>> correctGroups = null;
        if (includeCorrectGroups) {
            correctGroups = groups.stream()
                    .map(WordGroup::words)
                    .collect(Collectors.toUnmodifiableList());
        }
        return new GameInfoData(
                gameId,
                expiresAt,
                shuffledWords,
                summary.correctGuesses(),
                summary.wrongGuesses(),
                correctGroups
        );
    }
}