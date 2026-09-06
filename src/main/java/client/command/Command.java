package client.command;

import java.io.IOException;
import java.util.List;

public interface Command {
  String execute(List<String> args, CommandContext context) throws CommandException, IOException;
}
