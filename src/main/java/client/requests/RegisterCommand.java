package client.requests;

import client.Command;
import client.CommandContext;
import client.CommandException;
import client.OutputFormatter;
import shared.dto.ApiResponse;
import shared.dto.RegisterData;
import shared.dto.RegisterRequest;

import java.io.IOException;
import java.util.List;

public final class RegisterCommand implements Command {
    @Override
    public String execute(List<String> args, CommandContext context) throws CommandException, IOException {
        if (args.size() != 3) {
            throw new CommandException("Usage: register <username> <password>");
        }
        @SuppressWarnings("unchecked")
        ApiResponse<RegisterData> response = (ApiResponse<RegisterData>) context.connectionManager().send(
            new RegisterRequest(args.get(1), args.get(2))
        );
        if (!response.success()) {
            throw new CommandException(OutputFormatter.formatError(response.error().message()));
        }
        return "Registered: " + response.data().username();
    }
}
