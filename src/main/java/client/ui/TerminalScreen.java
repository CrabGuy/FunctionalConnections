package client.ui;

public final class TerminalScreen {
    private TerminalScreen() {}

    public static void clear() {
        System.out.print("\033[2J\033[H");
        System.out.flush();
    }

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