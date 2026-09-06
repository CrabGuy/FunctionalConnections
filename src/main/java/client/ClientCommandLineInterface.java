package client;

import client.dto.ClientConfig;
import client.requests.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ClientCommandLineInterface implements CommandLineInterface {
    private final PrintStream output;
    private final BufferedReader input;
    private final CommandContext context;
    private final Map<String, Command> commands;

    public ClientCommandLineInterface(ClientConfig config,
                                      AccountSession session,
                                      ConnectionManager connectionManager,
                                      NotificationListener notificationListener) {
        this.output = System.out;
        this.input = new BufferedReader(new InputStreamReader(System.in));
        this.context = new CommandContext(session, connectionManager, notificationListener, config, output);
        this.commands = new LinkedHashMap<>();
        commands.put("register", new RegisterCommand());
        commands.put("update", new UpdateCredentialsCommand());
        commands.put("updatecredentials", new UpdateCredentialsCommand());
        commands.put("login", new LoginCommand());
        commands.put("logout", new LogoutCommand());
        commands.put("submit", new SubmitProposalCommand());
        commands.put("game", new RequestGameInfoCommand());
        commands.put("gameinfo", new RequestGameInfoCommand());
        commands.put("gamestats", new RequestGameStatsCommand());
        commands.put("game-stats", new RequestGameStatsCommand());
        commands.put("leaderboard", new RequestLeaderboardCommand());
        commands.put("playerstats", new RequestPlayerStatsCommand());
        commands.put("player-stats", new RequestPlayerStatsCommand());
    }

    @Override
    public void start() throws IOException {
        context.connectionManager().connect(context.config().serverAddress(), context.config().tcpPort());
        output.println("Connected to " + context.config().serverAddress() + ":" + context.config().tcpPort());
        printHelp();
        try {
            runLoop();
        } finally {
            context.notificationListener().stop();
            context.connectionManager().close();
        }
    }

    @Override
    public void processCommand(String commandLine) throws IOException {
        String trimmed = commandLine.trim();
        if (trimmed.isEmpty()) return;
        if (trimmed.equalsIgnoreCase("exit") || trimmed.equalsIgnoreCase("quit")) return;
        String[] tokens = trimmed.split("\\s+");
        handleTokens(tokens);
    }

    private void runLoop() throws IOException {
        while (true) {
            output.print("> ");
            output.flush();
            String line = input.readLine();
            if (line == null) return;
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.equalsIgnoreCase("exit") || trimmed.equalsIgnoreCase("quit")) return;
            String[] tokens = trimmed.split("\\s+");
            handleTokens(tokens);
        }
    }

    private void handleTokens(String[] tokens) throws IOException {
        if (tokens.length == 0) return;
        String commandName = tokens[0].toLowerCase();
        if (commandName.equals("help")) {
            printHelp();
            return;
        }
        Command command = commands.get(commandName);
        if (command == null) {
            output.println("Unknown command. Type 'help'.");
            return;
        }
        try {
            String result = command.execute(Arrays.asList(tokens), context);
            output.println(result);
        } catch (CommandException e) {
            output.println("Error: " + e.getMessage());
        } catch (IOException e) {
            output.println("Network error: " + e.getMessage());
            throw e;
        }
    }

    private void printHelp() {
        output.println("Commands:");
        output.println("  register <username> <password>");
        output.println("  update <oldUsername> <newUsername> <oldPassword> <newPassword>");
        output.println("  login <username> <password>");
        output.println("  logout");
        output.println("  submit <word1> <word2> <word3> <word4>");
        output.println("  game [gameId]");
        output.println("  gamestats [gameId]");
        output.println("  leaderboard");
        output.println("  leaderboard top <K>");
        output.println("  leaderboard player <name>");
        output.println("  playerstats");
        output.println("  help");
        output.println("  exit");
    }
}
