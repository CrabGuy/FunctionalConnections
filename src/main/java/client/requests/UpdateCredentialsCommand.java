package client.requests;

import client.Command;
import client.CommandContext;
import client.CommandException;
import client.OutputFormatter;
import shared.dto.ApiResponse;
import shared.dto.UpdateCredentialsData;
import shared.dto.UpdateCredentialsRequest;

import java.io.IOException;
import java.util.List;

public final class UpdateCredentialsCommand implements Command {
    @Override
    public String execute(List<String> args, CommandContext context) throws CommandException, IOException {
        if (args.size() != 5) {
            throw new CommandException("Usage: update <oldUsername> <newUsername> <oldPassword> <newPassword>");
        }
        @SuppressWarnings("unchecked")
        ApiResponse<UpdateCredentialsData> response = (ApiResponse<UpdateCredentialsData>) context.connectionManager().send(
            new UpdateCredentialsRequest(args.get(1), args.get(2), args.get(3), args.get(4))
        );
        if (!response.success()) {
            throw new CommandException(OutputFormatter.formatError(response.error().message()));
        }
        context.session().clear();
        context.notificationListener().stop();
        return "Credentials updated for: " + response.data().newUsername() + ". Please log in again.";
    }
}
