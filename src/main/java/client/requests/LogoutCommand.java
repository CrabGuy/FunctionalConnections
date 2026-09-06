package client.requests;

import client.Command;
import client.CommandContext;
import client.CommandException;
import client.OutputFormatter;
import shared.dto.ApiResponse;
import shared.dto.LogoutData;
import shared.dto.LogoutRequest;

import java.io.IOException;
import java.util.List;

public final class LogoutCommand implements Command {
    @Override
    public String execute(List<String> args, CommandContext context) throws CommandException, IOException {
        if (args.size() != 1) {
            throw new CommandException("Usage: logout");
        }
        if (context.session().accountToken() == null || context.session().accountToken().isBlank()) {
            throw new CommandException("You must be logged in for this command.");
        }
        @SuppressWarnings("unchecked")
        ApiResponse<LogoutData> response = (ApiResponse<LogoutData>) context.connectionManager().send(
            new LogoutRequest(context.session().accountToken())
        );
        if (!response.success()) {
            throw new CommandException(OutputFormatter.formatError(response.error().message()));
        }
        context.notificationListener().stop();
        context.session().clear();
        return "Logged out.";
    }
}
