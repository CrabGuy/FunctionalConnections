package client.requests;

import client.*;
import shared.dto.ApiResponse;
import shared.dto.GameInfoData;
import shared.dto.SubmitProposalRequest;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SubmitProposalCommand implements Command {
    @Override
    public String execute(List<String> args, CommandContext context) throws CommandException, IOException {
        if (args.size() != 5) {
            throw new CommandException("Usage: submit <word1> <word2> <word3> <word4>");
        }
        if (context.session().accountToken() == null || context.session().accountToken().isBlank()) {
            throw new CommandException("You must be logged in for this command.");
        }
        List<String> words = List.of(args.get(1), args.get(2), args.get(3), args.get(4));
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
            case CORRECT -> "Proposal: CORRECT";
            case WRONG -> "Proposal: WRONG";
            case UNCHANGED -> "Proposal accepted; state unchanged.";
        };
        return outcomeMessage + "\n" + OutputFormatter.formatGameInfo(data, System.currentTimeMillis());
    }
}
