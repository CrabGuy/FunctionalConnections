package client.command.requests;

import client.command.Command;
import client.command.CommandContext;
import client.command.CommandException;
import client.formatting.OutputFormatter;
import shared.dto.ApiResponse;
import shared.dto.GameInfoData;
import shared.dto.LoginData;
import shared.dto.LoginRequest;
import shared.dto.RequestGameInfoRequest;

import java.io.IOException;
import java.util.List;

public final class LoginCommand implements Command {
    @Override
    public String execute(List<String> args, CommandContext context) throws CommandException, IOException {
        if (args.size() != 3) {
            throw new CommandException("Usage: login <username> <password>");
        }
        if (context.session().accountToken() != null && !context.session().accountToken().isBlank()) {
            throw new CommandException("Already logged in. Use logout before logging in as another account.");
        }
        context.notificationListener().start(
            context.config().udpPort(),
            info -> {
                synchronized (context.output()) {
                    context.output().println();
                    context.output().println("=== GAME ENDED ===");
                    context.output().println(OutputFormatter.formatGameInfo(info, System.currentTimeMillis()));
                    context.output().print("> ");
                    context.output().flush();
                }
            }
        );
        @SuppressWarnings("unchecked")
        ApiResponse<LoginData> response = (ApiResponse<LoginData>) context.connectionManager().send(
            new LoginRequest(args.get(1), args.get(2), context.config().udpPort())
        );
        if (!response.success()) {
            context.notificationListener().stop();
            throw new CommandException(OutputFormatter.formatError(response.error().message()));
        }
        context.session().setAccountToken(response.data().accountToken());

        // Fetch and display current game info (but do not store it)
        @SuppressWarnings("unchecked")
        ApiResponse<GameInfoData> gameResponse = (ApiResponse<GameInfoData>) context.connectionManager().send(
            new RequestGameInfoRequest(context.session().accountToken(), null)
        );
        String gameInfoMessage = "";
        if (gameResponse.success()) {
            gameInfoMessage = OutputFormatter.formatGameInfo(gameResponse.data(), System.currentTimeMillis());
        } else {
            gameInfoMessage = "Warning: could not fetch current game info: " +
                    OutputFormatter.formatError(gameResponse.error().message());
        }
        return "Logged in as " + args.get(1) + ". Current game participation started.\n" + gameInfoMessage;
    }
}
