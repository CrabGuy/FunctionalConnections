package client.command;

public final class CommandUtils {
  private CommandUtils() {}

  public static Long parseOptionalLong(String raw, String fieldName) throws CommandException {
    try {
      return Long.parseLong(raw);
    } catch (NumberFormatException e) {
      throw new CommandException("Invalid " + fieldName + ": " + raw);
    }
  }
}
