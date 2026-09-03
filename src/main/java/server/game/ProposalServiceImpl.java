package server.game;

import server.account.AccountService;
import server.account.exceptions.InvalidTokenException;
import server.dto.AccountPrincipal;
import server.dto.GameWordGroups;
import server.dto.PlayerGame;
import server.dto.Proposal;
import server.dto.WordGroup;
import server.game.exceptions.GameNotCurrentException;
import server.game.exceptions.GameNotFoundException;
import server.game.exceptions.InvalidProposalException;
import server.game.exceptions.MalformedProposalException;
import server.game.exceptions.PlayerAlreadyCompletedGameException;
import server.game.exceptions.UnknownWordsInProposalException;
import server.game.exceptions.WordsAlreadyGroupedException;
import shared.dto.GameInfoData;

import java.util.ArrayList;
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

        // Check if player already finished
        if (isPlayerFinished(playerGame, game.groups())) {
            throw new PlayerAlreadyCompletedGameException(username, gameId);
        }

        // Validate proposal
        if (words == null || words.size() != 4) {
            throw new MalformedProposalException();
        }

        Set<String> proposalSet = Set.copyOf(words);
        Set<String> allWords = getAllWords(game);
        if (!allWords.containsAll(proposalSet)) {
            throw new UnknownWordsInProposalException();
        }

        // Check if any word already grouped correctly
        List<Set<String>> correctSoFar = extractCorrectGuesses(playerGame.proposals(), game.groups());
        for (String word : proposalSet) {
            boolean alreadyGrouped = correctSoFar.stream().anyMatch(set -> set.contains(word));
            if (alreadyGrouped) {
                throw new WordsAlreadyGroupedException();
            }
        }

        // Add proposal to player's list
        Proposal newProposal = new Proposal(proposalSet);
        List<Proposal> updatedProposals = new ArrayList<>(playerGame.proposals());
        updatedProposals.add(newProposal);
        PlayerGame updatedPlayerGame = new PlayerGame(username, gameId, updatedProposals);
        playerGameRepository.save(updatedPlayerGame);

        // Build response
        boolean includeCorrectGroups = isPlayerFinished(updatedPlayerGame, game.groups())
                || gameClock.isCompleted(gameId, System.currentTimeMillis());

        return buildGameInfoData(
                gameId,
                gameClock.expiresAt(gameId),
                shuffleWords(game, gameId),
                updatedPlayerGame,
                game.groups(),
                includeCorrectGroups
        );
    }

    @Override
    public GameInfoData getGameInfo(String accountToken, Long gameId)
            throws InvalidTokenException, GameNotFoundException {

        AccountPrincipal principal = accountService.resolve(accountToken);
        String username = principal.username();

        long effectiveGameId = (gameId == null)
                ? gameClock.currentGameId(System.currentTimeMillis())
                : gameId;

        if (!gameRepository.exists(effectiveGameId)) {
            throw new GameNotFoundException(effectiveGameId);
        }

        GameWordGroups game = gameRepository.loadById(effectiveGameId);
        PlayerGame playerGame = playerGameRepository.findOrCreate(username, effectiveGameId);

        boolean includeCorrectGroups = isPlayerFinished(playerGame, game.groups())
                || gameClock.isCompleted(effectiveGameId, System.currentTimeMillis());

        return buildGameInfoData(
                effectiveGameId,
                gameClock.expiresAt(effectiveGameId),
                shuffleWords(game, effectiveGameId),
                playerGame,
                game.groups(),
                includeCorrectGroups
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

        boolean includeCorrectGroups = isPlayerFinished(playerGame, game.groups())
                || gameClock.isCompleted(gameId, System.currentTimeMillis());

        return buildGameInfoData(
                gameId,
                gameClock.expiresAt(gameId),
                shuffleWords(game, gameId),
                playerGame,
                game.groups(),
                includeCorrectGroups
        );
    }

    // --- Pure helper methods (static, no side effects) ---

    private static List<String> shuffleWords(GameWordGroups game, long gameId) {
        List<String> allWords = game.groups().stream()
                .flatMap(g -> g.words().stream())
                .collect(Collectors.toList());

        // Deterministic shuffle using gameId as seed
        Random random = new Random(gameId);
        List<String> shuffled = new ArrayList<>(allWords);
        java.util.Collections.shuffle(shuffled, random);
        return List.copyOf(shuffled);
    }

    private static Set<String> getAllWords(GameWordGroups game) {
        return game.groups().stream()
                .flatMap(g -> g.words().stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static boolean isCorrectProposal(Set<String> proposalWords, List<WordGroup> groups) {
        for (WordGroup group : groups) {
            if (Set.copyOf(group.words()).equals(proposalWords)) {
                return true;
            }
        }
        return false;
    }

    private static List<Set<String>> extractCorrectGuesses(List<Proposal> proposals, List<WordGroup> groups) {
        return proposals.stream()
                .filter(p -> isCorrectProposal(p.words(), groups))
                .map(Proposal::words)
                .collect(Collectors.toUnmodifiableList());
    }

    private static List<Set<String>> extractWrongGuesses(List<Proposal> proposals, List<WordGroup> groups) {
        return proposals.stream()
                .filter(p -> !isCorrectProposal(p.words(), groups))
                .map(Proposal::words)
                .collect(Collectors.toUnmodifiableList());
    }

    private static boolean isPlayerFinished(PlayerGame playerGame, List<WordGroup> groups) {
        int correct = extractCorrectGuesses(playerGame.proposals(), groups).size();
        int wrong = extractWrongGuesses(playerGame.proposals(), groups).size();
        return correct >= 3 || wrong >= 4;
    }

    private static GameInfoData buildGameInfoData(
            long gameId,
            long expiresAt,
            List<String> shuffledWords,
            PlayerGame playerGame,
            List<WordGroup> groups,
            boolean includeCorrectGroups
    ) {
        List<Set<String>> correct = extractCorrectGuesses(playerGame.proposals(), groups);
        List<Set<String>> wrong = extractWrongGuesses(playerGame.proposals(), groups);

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
                correct,
                wrong,
                correctGroups
        );
    }
}