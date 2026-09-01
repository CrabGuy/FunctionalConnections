package client.states;

import client.ClientContext;
import client.ClientState;
import client.NetworkSession;
import shared.DataContracts;
import shared.Response;

import java.util.List;
import java.util.Scanner;

public final class PlayingState implements ClientState {
    @Override
    public void handle(Scanner scanner, ClientContext context) {
        var view = context.getView();
        var session = context.getSession();
        var observer = context.getObserver();

        String feedback = "";
        while (session.isLoggedIn() && !context.isExitRequested()) {
            if (observer.hasNewGame()) {
                view.clearScreen();
                System.out.println("\n[!] A new game has started! Returning to main menu...");
                view.pauseForUser(scanner);
                context.setState(new MainMenuState());
                return;
            }

            view.clearScreen();
            Response<DataContracts.GameStateDto> info = session.getGameInfo(null);
            if (!info.success() || info.result() == null) {
                System.out.println("✗ Could not load board: " + info.error());
                break;
            }

            DataContracts.GameStateDto board = info.result();
            view.renderGameBoard(board);

            if (!feedback.isBlank()) System.out.println("\n" + feedback);

            boolean isGameOver = board instanceof DataContracts.OngoingGameStateDto ongoing &&
                    ("WON".equalsIgnoreCase(ongoing.status()) || "LOST".equalsIgnoreCase(ongoing.status())) ||
                    board instanceof DataContracts.CompletedGameStateDto;

            if (isGameOver) {
                String status = board instanceof DataContracts.OngoingGameStateDto ongoing ? ongoing.status() :
                        ((DataContracts.CompletedGameStateDto) board).status();
                System.out.println("WON".equalsIgnoreCase(status)
                        ? "\n🎉 CONGRATULATIONS! You solved all groups and WON this game!"
                        : "\n❌ GAME OVER! You ran out of attempts or time.");
                view.pauseForUser(scanner);
                context.setState(new MainMenuState());
                return;
            }

            String input = view.promptProposal(scanner);
            if ("back".equalsIgnoreCase(input) || "exit".equalsIgnoreCase(input)) {
                context.setState(new MainMenuState());
                return;
            }

            if (observer.hasNewGame()) {
                view.clearScreen();
                System.out.println("\n[!] A new game has started! Returning to main menu...");
                view.pauseForUser(scanner);
                context.setState(new MainMenuState());
                return;
            }

            List<String> words = view.parseProposalInput(input, ((DataContracts.OngoingGameStateDto) board).remainingWords());
            if (words.size() != 4) {
                feedback = "✗ Invalid input! Please provide 4 valid words or indices.";
                continue;
            }

            Response<DataContracts.ProposalOutcomeDto> outcome = session.submitProposal(words);
            if (outcome.success() && outcome.result() != null) {
                boolean correct = outcome.result().lastGuessCorrect();
                feedback = correct ? "✓ Correct group found!" : "✗ Incorrect group suggestion.";
                if (((DataContracts.OngoingGameStateDto) board).solvedGroups().size() == 2 && correct) {
                    if (view.confirmAutoSolve(scanner)) {
                        autoSolveLastGroup(session);
                    }
                }
            } else {
                feedback = "✗ Proposal Rejected: " + outcome.error();
            }
        }
        // If loop exits without setting state, go to main menu
        context.setState(new MainMenuState());
    }

    private void autoSolveLastGroup(NetworkSession session) {
        Response<DataContracts.GameStateDto> info = session.getGameInfo(null);
        if (info.success() && info.result() instanceof DataContracts.OngoingGameStateDto ongoing &&
                ongoing.remainingWords().size() == 4) {
            session.submitProposal(ongoing.remainingWords());
        }
    }
}