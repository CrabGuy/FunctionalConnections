package client.command.requests;

import client.command.Command;
import client.command.CommandContext;
import client.command.CommandException;
import client.command.CommandUtils;
import client.formatting.OutputFormatter;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import shared.dto.ApiResponse;
import shared.dto.GameStatsData;
import shared.dto.RequestGameStatsRequest;

public final class RequestGameStatsCommand implements Command {
  @Override
  public String execute(List<String> args, CommandContext context)
      throws CommandException, IOException {
    if (args.size() > 2) {
      throw new CommandException("Usage: gamestats [gameId]");
    }
    if (!context.session().isLoggedIn()) {
      throw new CommandException("You must be logged in for this command.");
    }

    Optional<Long> gameId =
        args.size() == 2
            ? Optional.of(CommandUtils.parseOptionalLong(args.get(1), "gameId"))
            : Optional.empty();

    @SuppressWarnings("unchecked")
    ApiResponse<GameStatsData> response =
        (ApiResponse<GameStatsData>)
            context
                .connectionManager()
                .send(
                    new RequestGameStatsRequest(
                        context.session().accountToken(), gameId.orElse(null)));

    if (!response.success()) {
      throw new CommandException(OutputFormatter.formatError(response.error().message()));
    }
    return OutputFormatter.formatGameStats(response.data(), System.currentTimeMillis());
  }
}
