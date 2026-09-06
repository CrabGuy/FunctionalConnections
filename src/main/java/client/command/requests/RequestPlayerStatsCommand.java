package client.command.requests;

import client.command.Command;
import client.command.CommandContext;
import client.command.CommandException;
import client.formatting.OutputFormatter;
import shared.dto.ApiResponse;
import shared.dto.PlayerStatsData;
import shared.dto.RequestPlayerStatsRequest;

import java.io.IOException;
import java.util.List;

public final class RequestPlayerStatsCommand implements Command {
    @Override
    public String execute(List<String> args, CommandContext context) throws CommandException, IOException {
        if (args.size() != 1) {
            throw new CommandException("Usage: playerstats");
        }
        if (context.session().accountToken() == null || context.session().accountToken().isBlank()) {
            throw new CommandException("You must be logged in for this command.");
        }
        @SuppressWarnings("unchecked")
        ApiResponse<PlayerStatsData> response = (ApiResponse<PlayerStatsData>) context.connectionManager().send(
            new RequestPlayerStatsRequest(context.session().accountToken())
        );
        if (!response.success()) {
            throw new CommandException(OutputFormatter.formatError(response.error().message()));
        }
        return OutputFormatter.formatPlayerStats(response.data());
    }
}
