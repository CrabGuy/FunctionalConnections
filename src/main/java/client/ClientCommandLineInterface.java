package client;

import client.dto.ClientConfig;
import shared.dto.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;
import java.util.Objects;

public record ClientCommandLineInterface(
        PrintStream output,
        BufferedReader input,
        ClientConfig config,
        AccountSession session,
        ConnectionManager connectionManager,
        NotificationListener notificationListener
) implements CommandLineInterface {

    public ClientCommandLineInterface(ClientConfig config,
                                      AccountSession session,
                                      ConnectionManager connectionManager,
                                      NotificationListener notificationListener) {
        this(System.out,
             new BufferedReader(new InputStreamReader(System.in)),
             config,
             session,
             connectionManager,
             notificationListener);
    }

    public ClientCommandLineInterface {
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(connectionManager, "connectionManager");
        Objects.requireNonNull(notificationListener, "notificationListener");
    }

    @Override
    public void start() throws IOException {
        connectionManager.connect(config.serverAddress(), config.tcpPort());
        println("Connected to " + config.serverAddress() + ":" + config.tcpPort());
        printHelp();
        try {
            runLoop();
        } finally {
            notificationListener.stop();
            connectionManager.close();
        }
    }

    @Override
    public void processCommand(String command) throws IOException {
        List<String> args = CommandTokenizer.tokenize(command);
        if (args.isEmpty()) return;

        switch (args.get(0).toLowerCase()) {
            case "help" -> printHelp();
            case "register" -> register(args);
            case "update", "updatecredentials" -> updateCredentials(args);
            case "login" -> login(args);
            case "logout" -> logout();
            case "submit" -> submit(args);
            case "game", "gameinfo" -> gameInfo(args);
            case "gamestats", "game-stats" -> gameStats(args);
            case "leaderboard" -> leaderboard(args);
            case "playerstats", "player-stats" -> playerStats();
            default -> throw new IllegalArgumentException("Unknown command. Type 'help'.");
        }
    }

    private void runLoop() throws IOException {
        while (true) {
            output.print("> ");
            output.flush();
            String line = input.readLine();
            if (line == null) return;
            String command = line.trim();
            if (command.isEmpty()) continue;
            if (command.equalsIgnoreCase("exit") || command.equalsIgnoreCase("quit")) return;
            try {
                processCommand(command);
            } catch (IllegalArgumentException e) {
                println("Error: " + e.getMessage());
            } catch (IOException e) {
                println("Network error: " + e.getMessage());
                throw e;
            }
        }
    }

    private void register(List<String> args) throws IOException {
        requireSize(args, 3, "register <username> <password>");
        ApiResponse<?> response = connectionManager.send(new RegisterRequest(args.get(1), args.get(2)));
        if (!printResponseIfError(response)) return;
        RegisterData data = (RegisterData) response.data();
        println("Registered: " + data.username());
    }

    private void updateCredentials(List<String> args) throws IOException {
        requireSize(args, 5, "update <oldUsername> <newUsername> <oldPassword> <newPassword>");
        ApiResponse<?> response = connectionManager.send(new UpdateCredentialsRequest(
                args.get(1), args.get(2), args.get(3), args.get(4)));
        if (!printResponseIfError(response)) return;
        UpdateCredentialsData data = (UpdateCredentialsData) response.data();
        session.clear();
        notificationListener.stop();
        println("Credentials updated for: " + data.newUsername() + ". Please log in again.");
    }

    private void login(List<String> args) throws IOException {
        requireSize(args, 3, "login <username> <password>");
        requireLoggedOut();
        notificationListener.start(config.udpPort(), this::printGameEndNotification);
        ApiResponse<?> response = connectionManager.send(new LoginRequest(
                args.get(1), args.get(2), config.udpPort()));
        if (!printResponseIfError(response)) {
            notificationListener.stop();
            return;
        }
        LoginData data = (LoginData) response.data();
        session.setAccountToken(data.accountToken());
        println("Logged in as " + args.get(1) + ". Current game participation has started.");
    }

    private void logout() throws IOException {
        requireLoggedIn();
        ApiResponse<?> response = connectionManager.send(new LogoutRequest(session.accountToken()));
        if (!printResponseIfError(response)) return;
        notificationListener.stop();
        session.clear();
        println("Logged out.");
    }

    private void submit(List<String> args) throws IOException {
        requireLoggedIn();
        requireSize(args, 5, "submit <word1> <word2> <word3> <word4>");
        List<String> words = List.of(args.get(1), args.get(2), args.get(3), args.get(4));
        if (words.stream().distinct().count() != 4) {
            throw new IllegalArgumentException("A proposal must contain four distinct words.");
        }
        ApiResponse<?> response = connectionManager.send(new SubmitProposalRequest(session.accountToken(), words));
        if (!printResponseIfError(response)) return;
        GameInfoData data = (GameInfoData) response.data();
        boolean correct = GameInfoCalculator.containsGuess(data, words, true);
        boolean wrong = !correct && GameInfoCalculator.containsGuess(data, words, false);
        println(correct ? "Proposal: CORRECT" : wrong ? "Proposal: WRONG" : "Proposal accepted; state unchanged.");
        printGameInfo(data);
    }

    private void gameInfo(List<String> args) throws IOException {
        requireLoggedIn();
        if (args.size() > 2) throw new IllegalArgumentException("Usage: game [gameId]");
        Long gameId = args.size() == 2 ? parseLong(args.get(1), "gameId") : null;
        ApiResponse<?> response = connectionManager.send(new RequestGameInfoRequest(session.accountToken(), gameId));
        if (!printResponseIfError(response)) return;
        printGameInfo((GameInfoData) response.data());
    }

    private void gameStats(List<String> args) throws IOException {
        requireLoggedIn();
        if (args.size() > 2) throw new IllegalArgumentException("Usage: gamestats [gameId]");
        Long gameId = args.size() == 2 ? parseLong(args.get(1), "gameId") : null;
        ApiResponse<?> response = connectionManager.send(new RequestGameStatsRequest(session.accountToken(), gameId));
        if (!printResponseIfError(response)) return;
        println(OutputFormatter.formatGameStats((GameStatsData) response.data()));
    }

    private void leaderboard(List<String> args) throws IOException {
        requireLoggedIn();
        if (args.size() == 1 || args.get(1).equalsIgnoreCase("all")) {
            requestLeaderboard(null, null);
            return;
        }
        if (args.size() == 3 && args.get(1).equalsIgnoreCase("top")) {
            int topK = parseInt(args.get(2), "topK");
            if (topK <= 0) throw new IllegalArgumentException("topK must be greater than zero");
            requestLeaderboard(null, topK);
            return;
        }
        if (args.size() == 3 && args.get(1).equalsIgnoreCase("player")) {
            requestLeaderboard(args.get(2), null);
            return;
        }
        throw new IllegalArgumentException("Usage: leaderboard | leaderboard top <K> | leaderboard player <name>");
    }

    private void requestLeaderboard(String playerName, Integer topK) throws IOException {
        ApiResponse<?> response = connectionManager.send(
                new RequestLeaderboardRequest(session.accountToken(), playerName, topK));
        if (!printResponseIfError(response)) return;
        println(OutputFormatter.formatLeaderboard((LeaderboardData) response.data()));
    }

    private void playerStats() throws IOException {
        requireLoggedIn();
        ApiResponse<?> response = connectionManager.send(new RequestPlayerStatsRequest(session.accountToken()));
        if (!printResponseIfError(response)) return;
        println(OutputFormatter.formatPlayerStats((PlayerStatsData) response.data()));
    }

    private void printGameInfo(GameInfoData data) {
        println(OutputFormatter.formatGameInfo(data, System.currentTimeMillis()));
    }

    private void printGameEndNotification(GameInfoData data) {
        synchronized (output) {
            output.println();
            output.println("=== GAME ENDED ===");
            printGameInfo(data);
            output.print("> ");
            output.flush();
        }
    }

    private void printHelp() {
        println("Commands:");
        println("  register <username> <password>");
        println("  update <oldUsername> <newUsername> <oldPassword> <newPassword>");
        println("  login <username> <password>");
        println("  logout");
        println("  submit <word1> <word2> <word3> <word4>");
        println("  game [gameId]");
        println("  gamestats [gameId]");
        println("  leaderboard");
        println("  leaderboard top <K>");
        println("  leaderboard player <name>");
        println("  playerstats");
        println("  help");
        println("  exit");
    }

    // Returns true if no error (i.e., success), false if error was printed
    private boolean printResponseIfError(ApiResponse<?> response) {
        if (!response.success()) {
            ApiError error = response.error();
            String message = error == null ? "unknown server error" : error.code() + " - " + error.message();
            println(OutputFormatter.formatError(message));
            return false;
        }
        return true;
    }

    private void requireLoggedIn() {
        if (session.accountToken() == null || session.accountToken().isBlank()) {
            throw new IllegalArgumentException("You must be logged in for this command.");
        }
    }

    private void requireLoggedOut() {
        if (session.accountToken() != null && !session.accountToken().isBlank()) {
            throw new IllegalArgumentException("Already logged in. Use logout before logging in as another account.");
        }
    }

    private static void requireSize(List<String> args, int size, String usage) {
        if (args.size() != size) {
            throw new IllegalArgumentException("Usage: " + usage);
        }
    }

    private static int parseInt(String value, String field) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + field + ": " + value);
        }
    }

    private static long parseLong(String value, String field) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + field + ": " + value);
        }
    }

    private void println(String message) {
        synchronized (output) {
            output.println(message);
        }
    }
}