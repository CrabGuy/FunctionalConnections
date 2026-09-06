package client.command.requests;

import client.command.Command;
import client.command.CommandContext;
import client.command.CommandException;
import client.formatting.OutputFormatter;
import java.io.IOException;
import java.util.List;
import shared.dto.ApiResponse;
import shared.dto.LeaderboardData;
import shared.dto.RequestLeaderboardRequest;

public final class RequestLeaderboardCommand implements Command {
  @Override
  public String execute(List<String> args, CommandContext context)
      throws CommandException, IOException {
    if (context.session().accountToken() == null || context.session().accountToken().isBlank()) {
      throw new CommandException("You must be logged in for this command.");
    }
    if (args.size() == 1 || (args.size() == 2 && args.get(1).equalsIgnoreCase("all"))) {
      return requestLeaderboard(context, null, null);
    }
    if (args.size() == 3 && args.get(1).equalsIgnoreCase("top")) {
      int topK;
      try {
        topK = Integer.parseInt(args.get(2));
      } catch (NumberFormatException e) {
        throw new CommandException("Invalid topK: " + args.get(2));
      }
      if (topK <= 0) {
        throw new CommandException("topK must be greater than zero");
      }
      return requestLeaderboard(context, null, topK);
    }
    if (args.size() == 3 && args.get(1).equalsIgnoreCase("player")) {
      return requestLeaderboard(context, args.get(2), null);
    }
    throw new CommandException(
        "Usage: leaderboard | leaderboard top <K> | leaderboard player <name>");
  }

  private String requestLeaderboard(CommandContext context, String playerName, Integer topK)
      throws CommandException, IOException {
    @SuppressWarnings("unchecked")
    ApiResponse<LeaderboardData> response =
        (ApiResponse<LeaderboardData>)
            context
                .connectionManager()
                .send(
                    new RequestLeaderboardRequest(
                        context.session().accountToken(), playerName, topK));
    if (!response.success()) {
      throw new CommandException(OutputFormatter.formatError(response.error().message()));
    }
    return OutputFormatter.formatLeaderboard(response.data());
  }
}
