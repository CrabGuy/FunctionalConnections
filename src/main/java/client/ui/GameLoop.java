package client.ui;

import client.connection.ConnectionManager;
import client.formatting.GameEndReporter;
import client.formatting.GameInfoCalculator;
import client.formatting.OutputFormatter;
import client.session.AccountSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import shared.dto.*;

/**
 * Handles the interactive gameplay loop: displaying game info, accepting proposals, and showing
 * results until the game ends or the user quits.
 */
public final class GameLoop {

  private final ConnectionManager connectionManager;
  private final AccountSession session;
  private final GameEndNotifier notifier;
  private final PrintStream output;
  private final BufferedReader input;

  /**
   * Constructs a new game loop.
   *
   * @param connectionManager the connection manager
   * @param session the account session
   * @param notifier the game end notifier
   * @param output the output stream for user messages
   * @param input the input reader for user commands
   */
  public GameLoop(
      ConnectionManager connectionManager,
      AccountSession session,
      GameEndNotifier notifier,
      PrintStream output,
      BufferedReader input) {
    this.connectionManager = connectionManager;
    this.session = session;
    this.notifier = notifier;
    this.output = output;
    this.input = input;
  }

  /**
   * Runs the game loop until the user quits or the game ends.
   *
   * @throws IOException if an I/O error occurs
   */
  public void run() throws IOException {
    while (true) {
      if (notifier.consumeIfPending()) {
        showGameSummary(notifier.getGameId());
        pressEnterToContinue();
        continue;
      }

      ApiResponse<GameInfoData> gameResponse =
          ClientActions.fetchGameInfo(connectionManager, session.accountToken(), null);
      if (!gameResponse.success()) {
        output.println("Error fetching game: " + gameResponse.error().message());
        pressEnterToContinue();
        continue;
      }

      GameInfoData game = gameResponse.data();
      String status = GameInfoCalculator.status(game, System.currentTimeMillis());
      if (!status.equals("ACTIVE")) {
        showGameSummary(game.gameId());
        pressEnterToContinue();
        continue;
      }

      TerminalScreen.clear();
      output.println(OutputFormatter.formatGameInfoForPlay(game, System.currentTimeMillis()));
      output.println();
      output.print("Enter 4 words or numbers (or 'quit' to return to menu): ");
      output.flush();

      String line = input.readLine();
      if (line == null) return;

      String trimmed = line.trim();
      if (trimmed.isEmpty()) continue;
      if (trimmed.equalsIgnoreCase("quit")) {
        return;
      }
      if (trimmed.equalsIgnoreCase("help")) {
        printGameHelp();
        continue;
      }

      try {
        List<String> words = parseProposal(trimmed, game);
        submitProposal(words);
      } catch (IllegalArgumentException e) {
        output.println("Error: " + e.getMessage());
        pressEnterToContinue();
      }
    }
  }

  /**
   * Submits a proposal to the server and displays the result.
   *
   * @param words the proposed words
   * @throws IOException if an I/O error occurs
   */
  private void submitProposal(List<String> words) throws IOException {
    @SuppressWarnings("unchecked")
    ApiResponse<GameInfoData> response =
        (ApiResponse<GameInfoData>)
            connectionManager.send(new SubmitProposalRequest(session.accountToken(), words));
    if (!response.success()) {
      output.println("Proposal failed: " + response.error().message());
      pressEnterToContinue();
      return;
    }

    GameInfoData data = response.data();
    GameInfoCalculator.ProposalOutcome outcome = GameInfoCalculator.evaluateProposal(data, words);
    String outcomeMessage =
        switch (outcome) {
          case CORRECT -> "Proposal: CORRECT";
          case WRONG -> "Proposal: WRONG";
          case UNCHANGED -> "Proposal accepted; state unchanged.";
        };
    output.println(outcomeMessage);
    output.println();
    output.println(OutputFormatter.formatGameInfoForPlay(data, System.currentTimeMillis()));

    String status = GameInfoCalculator.status(data, System.currentTimeMillis());
    if (status.equals("WON") || status.equals("LOST")) {
      output.println();
      output.println(
          GameEndReporter.fetchGameStatsOnly(
              connectionManager, session.accountToken(), data.gameId()));
      pressEnterToContinue();
    } else {
      pressEnterToContinue();
    }
  }

  /**
   * Displays the summary of a completed game.
   *
   * @param gameId the game ID
   * @throws IOException if an I/O error occurs
   */
  private void showGameSummary(long gameId) throws IOException {
    TerminalScreen.clear();
    output.println(
        GameEndReporter.fetchAndFormat(connectionManager, session.accountToken(), gameId));
  }

  /**
   * Prompts the user to press Enter to continue.
   *
   * @throws IOException if an I/O error occurs
   */
  private void pressEnterToContinue() throws IOException {
    output.println();
    output.print("Press Enter to continue...");
    output.flush();
    input.readLine();
  }

  /**
   * Prints game help instructions.
   *
   * @throws IOException if an I/O error occurs
   */
  private void printGameHelp() throws IOException {
    TerminalScreen.clear();
    output.println("Game Help:");
    output.println("  - Enter 4 words separated by spaces to submit a proposal.");
    output.println(
        "  - Alternatively, enter 4 numbers corresponding to the indices of remaining words.");
    output.println("  - 'quit' returns to the logged-in menu.");
    output.println("  - 'help' shows this help.");
    pressEnterToContinue();
  }

  /**
   * Parses a user input line into a list of exactly four distinct words or numbers that map to
   * remaining words.
   *
   * @param input the raw user input
   * @param game the current game info (to map numbers to words)
   * @return the list of four words
   * @throws IllegalArgumentException if the input is malformed
   */
  private List<String> parseProposal(String input, GameInfoData game) {
    String[] parts = input.split("\\s+");
    if (parts.length != 4) {
      throw new IllegalArgumentException("A proposal must contain exactly 4 words or numbers.");
    }

    boolean allNumbers = true;
    for (String part : parts) {
      try {
        Integer.parseInt(part);
      } catch (NumberFormatException e) {
        allNumbers = false;
        break;
      }
    }

    if (allNumbers) {
      List<String> remainingWords = GameInfoCalculator.remainingWords(game);
      List<Integer> indices = new ArrayList<>();
      for (String part : parts) {
        int idx = Integer.parseInt(part) - 1;
        if (idx < 0 || idx >= remainingWords.size()) {
          throw new IllegalArgumentException(
              "Invalid word number: "
                  + (idx + 1)
                  + ". There are "
                  + remainingWords.size()
                  + " remaining words.");
        }
        indices.add(idx);
      }
      return indices.stream().map(remainingWords::get).toList();
    }

    Set<String> unique = Set.of(parts);
    if (unique.size() != 4) {
      throw new IllegalArgumentException("Words must be distinct.");
    }
    return List.of(parts);
  }
}
