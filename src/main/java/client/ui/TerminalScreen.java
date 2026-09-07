package client.ui;

/**
 * Utility class for terminal screen operations such as clearing the screen and printing a title
 * banner.
 */
public final class TerminalScreen {

  private TerminalScreen() {
    // Prevent instantiation
  }

  /** Clears the terminal screen using ANSI escape codes. */
  public static void clear() {
    System.out.print("\033[2J\033[H");
    System.out.flush();
  }

  /** Prints the application title banner. */
  public static void printTitle() {
    String title = "Functional Connections";
    String boldCyan = "\033[1;36m";
    String reset = "\033[0m";
    String border = boldCyan + "+" + "-".repeat(title.length() + 2) + "+" + reset;
    System.out.println(border);
    System.out.println(boldCyan + "| " + title + " |" + reset);
    System.out.println(border);
  }
}
