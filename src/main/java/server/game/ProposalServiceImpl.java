package server.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import server.account.AccountService;
import server.account.exceptions.InvalidTokenException;
import server.dto.AccountPrincipal;
import server.dto.GameWordGroups;
import server.dto.PlayerGame;
import server.dto.PlayerGameKey;
import server.dto.Proposal;
import server.dto.WordGroup;
import server.game.exceptions.*;
import shared.dto.GameInfoData;
import shared.game.GameRules;

/**
 * Implementation of {@link ProposalService} that validates proposals and maintains per-player-game
 * state with proper locking.
 */
public final class ProposalServiceImpl implements ProposalService {

  private final AccountService accountService;
  private final GameRepository gameRepository;
  private final GameClock gameClock;
  private final PlayerGameRepository playerGameRepository;
  private final ConcurrentHashMap<PlayerGameKey, Object> locks = new ConcurrentHashMap<>();

  /**
   * Constructs the service with required dependencies.
   *
   * @param accountService the account service
   * @param gameRepository the game repository
   * @param gameClock the game clock
   * @param playerGameRepository the player game repository
   */
  public ProposalServiceImpl(
      AccountService accountService,
      GameRepository gameRepository,
      GameClock gameClock,
      PlayerGameRepository playerGameRepository) {
    this.accountService = accountService;
    this.gameRepository = gameRepository;
    this.gameClock = gameClock;
    this.playerGameRepository = playerGameRepository;
  }

  /** {@inheritDoc} */
  @Override
  public GameInfoData submitProposal(String accountToken, long gameId, List<String> words)
      throws InvalidTokenException,
          InvalidProposalException,
          GameNotCurrentException,
          PlayerAlreadyCompletedGameException {
    AccountPrincipal principal = accountService.resolve(accountToken);
    String username = principal.username();
    long currentGameId = gameClock.currentGameId(System.currentTimeMillis());
    if (gameId != currentGameId) {
      throw new GameNotCurrentException(gameId, currentGameId);
    }

    PlayerGameKey key = new PlayerGameKey(username, gameId);
    Object lock = locks.computeIfAbsent(key, k -> new Object());
    synchronized (lock) {
      GameWordGroups game = gameRepository.loadById(gameId);
      PlayerGame playerGame = playerGameRepository.findOrCreate(username, gameId);
      GuessesSummary summary = summarize(playerGame, game.groups());

      if (GameRules.isWon(summary.correctGuesses().size())
          || GameRules.isLost(summary.wrongGuesses().size())) {
        throw new PlayerAlreadyCompletedGameException(username, gameId);
      }

      validateProposal(words, game, summary);

      Proposal newProposal = new Proposal(Set.copyOf(words));
      List<Proposal> updatedProposals = new ArrayList<>(playerGame.proposals());
      updatedProposals.add(newProposal);
      PlayerGame updatedPlayerGame = new PlayerGame(username, gameId, updatedProposals);
      playerGameRepository.save(updatedPlayerGame);

      GuessesSummary updatedSummary = summarize(updatedPlayerGame, game.groups());
      boolean includeCorrectGroups =
          GameRules.isWon(updatedSummary.correctGuesses().size())
              || GameRules.isLost(updatedSummary.wrongGuesses().size())
              || gameClock.isCompleted(gameId, System.currentTimeMillis());

      return buildGameInfoData(
          gameId,
          gameClock.expiresAt(gameId),
          GameLogic.shuffledWords(game, gameId),
          updatedPlayerGame,
          game.groups(),
          includeCorrectGroups,
          updatedSummary);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void touchCurrentGame(String accountToken) throws InvalidTokenException {
    AccountPrincipal principal = accountService.resolve(accountToken);
    long currentGameId = gameClock.currentGameId(System.currentTimeMillis());
    playerGameRepository.findOrCreate(principal.username(), currentGameId);
  }

  /** {@inheritDoc} */
  @Override
  public GameInfoData getGameInfo(String accountToken, Long gameId)
      throws InvalidTokenException, GameNotFoundException {
    AccountPrincipal principal = accountService.resolve(accountToken);
    String username = principal.username();
    long currentGameId = gameClock.currentGameId(System.currentTimeMillis());
    if (gameId != null && gameId > currentGameId) {
      throw new GameNotFoundException(gameId);
    }

    long effectiveGameId =
        (gameId == null) ? gameClock.currentGameId(System.currentTimeMillis()) : gameId;
    if (!gameRepository.exists(effectiveGameId)) {
      throw new GameNotFoundException(effectiveGameId);
    }

    GameWordGroups game = gameRepository.loadById(effectiveGameId);
    PlayerGame playerGame = playerGameRepository.findOrCreate(username, effectiveGameId);
    GuessesSummary summary = summarize(playerGame, game.groups());
    boolean includeCorrectGroups =
        GameRules.isWon(summary.correctGuesses().size())
            || GameRules.isLost(summary.wrongGuesses().size())
            || gameClock.isCompleted(effectiveGameId, System.currentTimeMillis());

    return buildGameInfoData(
        effectiveGameId,
        gameClock.expiresAt(effectiveGameId),
        GameLogic.shuffledWords(game, effectiveGameId),
        playerGame,
        game.groups(),
        includeCorrectGroups,
        summary);
  }

  /**
   * Validates that the proposal is structurally correct and consists of ungrouped words.
   *
   * @param words the proposed words
   * @param game the game word groups
   * @param summary the current guesses summary
   * @throws InvalidProposalException if the proposal is invalid
   */
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
      boolean alreadyGrouped =
          summary.correctGuesses().stream().anyMatch(set -> set.contains(word));
      if (alreadyGrouped) {
        throw new WordsAlreadyGroupedException();
      }
    }
  }

  /**
   * A summary of correct and wrong guesses.
   *
   * @param correctGuesses list of correct guess sets
   * @param wrongGuesses list of wrong guess sets
   */
  private record GuessesSummary(List<Set<String>> correctGuesses, List<Set<String>> wrongGuesses) {}

  /**
   * Summarizes the player's proposals into correct and wrong guesses.
   *
   * @param playerGame the player game record
   * @param groups the correct word groups
   * @return a {@link GuessesSummary}
   */
  private static GuessesSummary summarize(PlayerGame playerGame, List<WordGroup> groups) {
    List<Set<String>> correct = new ArrayList<>();
    List<Set<String>> wrong = new ArrayList<>();
    for (Proposal proposal : playerGame.proposals()) {
      if (GameLogic.isCorrectProposal(proposal.words(), groups)) {
        correct.add(proposal.words());
      } else {
        wrong.add(proposal.words());
      }
    }
    return new GuessesSummary(correct, wrong);
  }

  /**
   * Returns the set of all words in the game.
   *
   * @param game the game word groups
   * @return a set of all words
   */
  private static Set<String> getAllWords(GameWordGroups game) {
    return game.groups().stream()
        .flatMap(g -> g.words().stream())
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Builds the {@link GameInfoData} DTO from the given parameters.
   *
   * @param gameId the game ID
   * @param expiresAt the expiration time
   * @param shuffledWords the shuffled word list
   * @param playerGame the player game record
   * @param groups the correct word groups
   * @param includeCorrectGroups whether to include correct groups in the DTO
   * @param summary the guesses summary
   * @return the built {@link GameInfoData}
   */
  private static GameInfoData buildGameInfoData(
      long gameId,
      long expiresAt,
      List<String> shuffledWords,
      PlayerGame playerGame,
      List<WordGroup> groups,
      boolean includeCorrectGroups,
      GuessesSummary summary) {
    List<List<String>> correctGroups = null;
    if (includeCorrectGroups) {
      correctGroups = groups.stream().map(WordGroup::words).toList();
    }
    return new GameInfoData(
        gameId,
        expiresAt,
        shuffledWords,
        summary.correctGuesses(),
        summary.wrongGuesses(),
        correctGroups);
  }
}
