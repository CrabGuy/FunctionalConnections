package client;

import client.dto.ClientConfig;
import shared.dto.ApiError;
import shared.dto.ApiResponse;
import shared.dto.GameInfoData;
import shared.dto.GameStatsData;
import shared.dto.LeaderboardData;
import shared.dto.LeaderboardEntry;
import shared.dto.LoginData;
import shared.dto.LoginRequest;
import shared.dto.LogoutRequest;
import shared.dto.PlayerStatsData;
import shared.dto.RegisterData;
import shared.dto.RegisterRequest;
import shared.dto.RequestGameInfoRequest;
import shared.dto.RequestGameStatsRequest;
import shared.dto.RequestLeaderboardRequest;
import shared.dto.RequestPlayerStatsRequest;
import shared.dto.SubmitProposalRequest;
import shared.dto.UpdateCredentialsData;
import shared.dto.UpdateCredentialsRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;
import java.util.Objects;

/** Console implementation of the client command set. */
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
        print("Connected to " + config.serverAddress() + ":" + config.tcpPort());
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
        if (args.isEmpty()) {
            return;
        }

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
            if (line == null) {
                return;
            }

            String command = line.trim();
            if (command.isEmpty()) {
                continue;
            }
            if (command.equalsIgnoreCase("exit") || command.equalsIgnoreCase("quit")) {
                return;
            }

            try {
                processCommand(command);
            } catch (IllegalArgumentException exception) {
                print("Error: " + exception.getMessage());
            } catch (IOException exception) {
                print("Network error: " + exception.getMessage());
                throw exception;
            }
        }
    }

    private void register(List<String> args) throws IOException {
        requireSize(args, 3, "register <username> <password>");
        ApiResponse<?> response = connectionManager.send(new RegisterRequest(args.get(1), args.get(2)));
        printResponse(response);
        if (response.success() && response.data() instanceof RegisterData data) {
            print("Registered: " + data.username());
        }
    }

    private void updateCredentials(List<String> args) throws IOException {
        requireSize(args, 5, "update <oldUsername> <newUsername> <oldPassword> <newPassword>");
        ApiResponse<?> response = connectionManager.send(new UpdateCredentialsRequest(
                args.get(1), args.get(2), args.get(3), args.get(4)));
        printResponse(response);
        if (response.success() && response.data() instanceof UpdateCredentialsData data) {
            session.clear();
            notificationListener.stop();
            print("Credentials updated for: " + data.newUsername() + ". Please log in again.");
        }
    }

    private void login(List<String> args) throws IOException {
        requireSize(args, 3, "login <username> <password>");
        requireLoggedOut();

        notificationListener.start(config.udpPort(), this::printGameEndNotification);
        ApiResponse<?> response = connectionManager.send(new LoginRequest(
                args.get(1), args.get(2), config.udpPort()));
        if (!response.success()) {
            notificationListener.stop();
        }

        printResponse(response);
        if (response.success() && response.data() instanceof LoginData data) {
            session.setAccountToken(data.accountToken());
            print("Logged in as " + args.get(1) + ". Current game participation has started.");
        }
    }

    private void logout() throws IOException {
        requireLoggedIn();
        ApiResponse<?> response = connectionManager.send(new LogoutRequest(session.accountToken()));
        printResponse(response);
        if (response.success()) {
            notificationListener.stop();
            session.clear();
            print("Logged out.");
        }
    }

    private void submit(List<String> args) throws IOException {
        requireLoggedIn();
        requireSize(args, 5, "submit <word1> <word2> <word3> <word4>");
        List<String> words = List.of(args.get(1), args.get(2), args.get(3), args.get(4));
        if (words.stream().distinct().count() != 4) {
            throw new IllegalArgumentException("A proposal must contain four distinct words.");
        }

        ApiResponse<?> response = connectionManager.send(
                new SubmitProposalRequest(session.accountToken(), words));
        printResponse(response);
        if (response.success() && response.data() instanceof GameInfoData data) {
            boolean correct = GameInfoCalculator.containsGuess(data, words, true);
            boolean wrong = !correct && GameInfoCalculator.containsGuess(data, words, false);
            print(correct ? "Proposal: CORRECT" : wrong ? "Proposal: WRONG" : "Proposal accepted; state unchanged.");
            printGameInfo(data);
        }
    }

    private void gameInfo(List<String> args) throws IOException {
        requireLoggedIn();
        if (args.size() > 2) {
            throw new IllegalArgumentException("Usage: game [gameId]");
        }

        Long gameId = args.size() == 2 ? parseLong(args.get(1), "gameId") : null;
        ApiResponse<?> response = connectionManager.send(
                new RequestGameInfoRequest(session.accountToken(), gameId));
        printResponse(response);
        if (response.success() && response.data() instanceof GameInfoData data) {
            printGameInfo(data);
        }
    }

    private void gameStats(List<String> args) throws IOException {
        requireLoggedIn();
        if (args.size() > 2) {
            throw new IllegalArgumentException("Usage: gamestats [gameId]");
        }

        Long gameId = args.size() == 2 ? parseLong(args.get(1), "gameId") : null;
        ApiResponse<?> response = connectionManager.send(
                new RequestGameStatsRequest(session.accountToken(), gameId));
        printResponse(response);
        if (response.success() && response.data() instanceof GameStatsData data) {
            printGameStats(data);
        }
    }

    private void leaderboard(List<String> args) throws IOException {
        requireLoggedIn();
        if (args.size() == 1 || args.get(1).equalsIgnoreCase("all")) {
            requestLeaderboard(null, null);
            return;
        }
        if (args.size() == 3 && args.get(1).equalsIgnoreCase("top")) {
            int topK = parseInt(args.get(2), "topK");
            if (topK <= 0) {
                throw new IllegalArgumentException("topK must be greater than zero");
            }
            requestLeaderboard(null, topK);
            return;
        }
        if (args.size() == 3 && args.get(1).equalsIgnoreCase("player")) {
            requestLeaderboard(args.get(2), null);
            return;
        }
        throw new IllegalArgumentException(
                "Usage: leaderboard | leaderboard top <K> | leaderboard player <name>");
    }

    private void requestLeaderboard(String playerName, Integer topK) throws IOException {
        ApiResponse<?> response = connectionManager.send(
                new RequestLeaderboardRequest(session.accountToken(), playerName, topK));
        printResponse(response);
        if (response.success() && response.data() instanceof LeaderboardData data) {
            printLeaderboard(data);
        }
    }

    private void playerStats() throws IOException {
        requireLoggedIn();
        ApiResponse<?> response = connectionManager.send(
                new RequestPlayerStatsRequest(session.accountToken()));
        printResponse(response);
        if (response.success() && response.data() instanceof PlayerStatsData data) {
            printPlayerStats(data);
        }
    }

    private void printResponse(ApiResponse<?> response) {
        if (!response.success()) {
            ApiError error = response.error();
            print("Request failed: " + (error == null
                    ? "unknown server error"
                    : error.code() + " - " + error.message()));
        }
    }

    private void printGameInfo(GameInfoData data) {
        long now = System.currentTimeMillis();
        print("Game " + data.gameId());
        print("Status: " + GameInfoCalculator.status(data, now));
        print("Time remaining: " + formatDuration(GameInfoCalculator.remainingTimeMillis(data, now)));
        print("Score: " + GameInfoCalculator.score(data));
        print("Correct proposals: " + GameInfoCalculator.correctProposalCount(data));
        print("Mistakes: " + GameInfoCalculator.mistakeCount(data));
        print("Remaining words: " + GameInfoCalculator.remainingWords(data));
        if (!data.correctGuesses().isEmpty()) {
            print("Correct guesses: " + data.correctGuesses());
        }
        if (!data.wrongGuesses().isEmpty()) {
            print("Wrong guesses: " + data.wrongGuesses());
        }
        if (data.correctGroups() != null) {
            print("Correct groups: " + data.correctGroups());
        }
    }

    private void printGameStats(GameStatsData data) {
        print("Game statistics for " + data.gameId());
        print("Completed: " + data.completed());
        print("Time remaining: " + formatDuration(
                Math.max(0L, data.expiresAt() - System.currentTimeMillis())));
        print("Total participants: " + data.totalParticipants());
        print("Active players: " + data.activePlayers());
        print("Completed players: " + data.completedPlayers());
        print("Winners: " + data.winners());
        print("Average score: " + data.averageScore());
    }

    private void printLeaderboard(LeaderboardData data) {
        print("Leaderboard (" + data.totalPlayers() + " players)");
        for (LeaderboardEntry entry : data.topPlayers()) {
            print(String.format("%d. %s — %d", entry.rank(), entry.username(), entry.score()));
        }
        if (data.requestedPlayer() != null) {
            LeaderboardEntry entry = data.requestedPlayer();
            print("Requested player: " + entry.username()
                    + " — rank " + entry.rank() + ", score " + entry.score());
        }
    }

    private void printPlayerStats(PlayerStatsData data) {
        print("Puzzles completed: " + data.puzzlesCompleted());
        print("Win rate: " + formatPercent(data.winRate()));
        print("Loss rate: " + formatPercent(data.lossRate()));
        print("Current streak: " + data.currentStreak());
        print("Max streak: " + data.maxStreak());
        print("Perfect puzzles: " + data.perfectPuzzles());
        print("Mistake histogram: " + data.mistakeHistogram());
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
        print("Commands:");
        print("  register <username> <password>");
        print("  update <oldUsername> <newUsername> <oldPassword> <newPassword>");
        print("  login <username> <password>");
        print("  logout");
        print("  submit <word1> <word2> <word3> <word4>");
        print("  game [gameId]");
        print("  gamestats [gameId]");
        print("  leaderboard");
        print("  leaderboard top <K>");
        print("  leaderboard player <name>");
        print("  playerstats");
        print("  help");
        print("  exit");
    }

    private void requireLoggedIn() {
        if (session.accountToken() == null || session.accountToken().isBlank()) {
            throw new IllegalArgumentException("You must be logged in for this command.");
        }
    }

    private void requireLoggedOut() {
        if (session.accountToken() != null && !session.accountToken().isBlank()) {
            throw new IllegalArgumentException(
                    "Already logged in. Use logout before logging in as another account.");
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
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid " + field + ": " + value);
        }
    }

    private static long parseLong(String value, String field) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid " + field + ": " + value);
        }
    }

    private static String formatDuration(long millis) {
        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return hours > 0
                ? String.format("%02d:%02d:%02d", hours, minutes, seconds)
                : String.format("%02d:%02d", minutes, seconds);
    }

    private static String formatPercent(double value) {
        return String.format("%.2f%%", value * 100.0);
    }

    private void print(String message) {
        synchronized (output) {
            output.println(message);
        }
    }
}
