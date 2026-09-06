package client.command.requests;

import client.command.Command;
import client.command.CommandContext;
import client.command.CommandException;
import client.formatting.AnsiColor;
import client.formatting.GameInfoCalculator;
import client.formatting.OutputFormatter;
import shared.dto.ApiResponse;
import shared.dto.GameInfoData;
import shared.dto.RequestGameInfoRequest;
import shared.dto.SubmitProposalRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SubmitProposalCommand implements Command {
    @Override
    public String execute(List<String> args, CommandContext context) throws CommandException, IOException {
        if (args.size() != 5) {
            throw new CommandException("Usage: submit <word1> <word2> <word3> <word4> OR submit <num1> <num2> <num3> <num4>");
        }
        if (context.session().accountToken() == null || context.session().accountToken().isBlank()) {
            throw new CommandException("You must be logged in for this command.");
        }

        List<String> words;
        boolean allNumbers = true;
        for (int i = 1; i <= 4; i++) {
            try {
                Integer.parseInt(args.get(i));
            } catch (NumberFormatException e) {
                allNumbers = false;
                break;
            }
        }

        if (allNumbers) {
            // Fetch current game info to resolve numbers to words
            @SuppressWarnings("unchecked")
            ApiResponse<GameInfoData> gameResponse = (ApiResponse<GameInfoData>) context.connectionManager().send(
                new RequestGameInfoRequest(context.session().accountToken(), null)
            );
            if (!gameResponse.success()) {
                throw new CommandException(OutputFormatter.formatError(gameResponse.error().message()));
            }
            GameInfoData currentGame = gameResponse.data();
            List<String> remainingWords = GameInfoCalculator.remainingWords(currentGame);
            words = new ArrayList<>();
            for (int i = 1; i <= 4; i++) {
                int index = Integer.parseInt(args.get(i)) - 1;
                if (index < 0 || index >= remainingWords.size()) {
                    throw new CommandException("Invalid word number: " + args.get(i) + ". There are " + remainingWords.size() + " remaining words.");
                }
                words.add(remainingWords.get(index));
            }
        } else {
            words = List.of(args.get(1), args.get(2), args.get(3), args.get(4));
        }

        Set<String> unique = new HashSet<>(words);
        if (unique.size() != 4) {
            throw new CommandException("A proposal must contain four distinct words.");
        }

        @SuppressWarnings("unchecked")
        ApiResponse<GameInfoData> response = (ApiResponse<GameInfoData>) context.connectionManager().send(
            new SubmitProposalRequest(context.session().accountToken(), words)
        );
        if (!response.success()) {
            throw new CommandException(OutputFormatter.formatError(response.error().message()));
        }

        GameInfoData data = response.data();
        GameInfoCalculator.ProposalOutcome outcome = GameInfoCalculator.evaluateProposal(data, words);
        String outcomeMessage = switch (outcome) {
            case CORRECT -> AnsiColor.GREEN.wrap("Proposal: CORRECT");
            case WRONG -> AnsiColor.RED.wrap("Proposal: WRONG");
            case UNCHANGED -> AnsiColor.YELLOW.wrap("Proposal accepted; state unchanged.");
        };
        return outcomeMessage + "\n" + OutputFormatter.formatGameInfo(data, System.currentTimeMillis());
    }
}
