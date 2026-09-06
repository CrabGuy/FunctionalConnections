package client.requests;

import client.Command;
import client.CommandContext;
import client.CommandException;
import client.OutputFormatter;
import shared.dto.ApiResponse;
import shared.dto.GameStatsData;
import shared.dto.RequestGameStatsRequest;

import java.io.IOException;
import java.util.List;

public final class RequestGameStatsCommand implements Command {
    @Override
    public String execute(List<String> args, CommandContext context) throws CommandException, IOException {
        if (args.size() > 2) {
            throw new CommandException("Usage: gamestats [gameId]");
        }
        if (context.session().accountToken() == null || context.session().accountToken().isBlank()) {
            throw new CommandException("You must be logged in for this command.");
        }
        Long gameId = null;
        if (args.size() == 2) {
            try {
                gameId = Long.parseLong(args.get(1));
            } catch (NumberFormatException e) {
                throw new CommandException("Invalid gameId: " + args.get(1));
            }
        }
        @SuppressWarnings("unchecked")
        ApiResponse<GameStatsData> response = (ApiResponse<GameStatsData>) context.connectionManager().send(
            new RequestGameStatsRequest(context.session().accountToken(), gameId)
        );
        if (!response.success()) {
            throw new CommandException(OutputFormatter.formatError(response.error().message()));
        }
        return OutputFormatter.formatGameStats(response.data(), System.currentTimeMillis());
    }
}
