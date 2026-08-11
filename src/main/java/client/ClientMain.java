package client;

import shared.JsonCodec;
import shared.Request;
import shared.Response;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Predicate;

public class ClientMain implements AutoCloseable {

    private final SocketChannel socketChannel;

    public ClientMain(String host, int port) throws IOException {
        this.socketChannel = SocketChannel.open(new InetSocketAddress(host, port));
    }

    public Response sendRequest(Request request) throws IOException {
        String payload = JsonCodec.serialize(request) + "\n";
        socketChannel.write(ByteBuffer.wrap(payload.getBytes(StandardCharsets.UTF_8)));

        ByteBuffer buffer = ByteBuffer.allocate(4096);
        int bytesRead = socketChannel.read(buffer);

        if (bytesRead == -1) {
            throw new IOException("Connection closed by server.");
        }

        buffer.flip();
        String rawResponse = StandardCharsets.UTF_8.decode(buffer).toString().trim();
        return JsonCodec.deserialize(rawResponse, Response.class);
    }

    @Override
    public void close() throws IOException {
        if (socketChannel != null && socketChannel.isOpen()) {
            socketChannel.close();
        }
    }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in);
             ClientMain client = new ClientMain("localhost", 8080)) {
            authLoop(client, scanner);
        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static void authLoop(ClientMain client, Scanner scanner) {
        clearScreen();
        System.out.println("==========================================");
        System.out.println("          === CONNECTIONS GAME ===");
        System.out.println("==========================================");
        System.out.println("1: Register");
        System.out.println("2: Login");
        System.out.println("3: Update Credentials");
        System.out.println("4: Exit");
        System.out.print("Choose action: ");

        String choice = scanner.nextLine().trim();

        if ("4".equals(choice)) {
            System.out.println("Goodbye!");
            return;
        }

        Optional<String> loggedUser = switch (choice) {
            case "1" -> handleRegister(client, scanner);
            case "2" -> handleLogin(client, scanner);
            case "3" -> {
                handleUpdateCredentials(client, scanner, null);
                pauseForUser(scanner);
                yield Optional.empty();
            }
            default -> {
                System.out.println("\n[!] Invalid option. Please try again.");
                pauseForUser(scanner);
                yield Optional.empty();
            }
        };

        loggedUser.ifPresentOrElse(
                username -> gameLoop(client, scanner, username),
                () -> authLoop(client, scanner)
        );
    }

    private static Optional<String> handleRegister(ClientMain client, Scanner scanner) {
        clearScreen();
        return readCredentials(scanner, "Register")
                .flatMap(creds -> {
                    var res = executeRequest(
                            client,
                            new Request.Register("register", creds.username(), creds.password()),
                            "Registration Result",
                            "Registration Failed"
                    );
                    if (res.map(Response::success).orElse(false)) {
                        return loginUser(client, creds.username(), creds.password());
                    } else {
                        pauseForUser(scanner);
                        return Optional.empty();
                    }
                });
    }

    private static Optional<String> handleLogin(ClientMain client, Scanner scanner) {
        clearScreen();
        return readCredentials(scanner, "Login")
                .flatMap(creds -> {
                    var userOpt = loginUser(client, creds.username(), creds.password());
                    if (userOpt.isEmpty()) {
                        pauseForUser(scanner);
                    }
                    return userOpt;
                });
    }

    private static Optional<String> loginUser(ClientMain client, String username, String password) {
        return executeRequest(
                client,
                new Request.Login("login", username, password),
                "Login Result",
                "Login Failed"
        ).filter(Response::success)
                .map(resp -> username);
    }

    private static void gameLoop(ClientMain client, Scanner scanner, String username) {
        clearScreen();
        System.out.println("==========================================");
        System.out.println("      --- MAIN MENU (" + username + ") ---");
        System.out.println("==========================================");
        fetchGameInfo(client);

        System.out.println("\nOptions:");
        System.out.println("1: Continuous Play Mode");
        System.out.println("2: Refresh Game Words & Info");
        System.out.println("3: Current Game Stats");
        System.out.println("4: Player Overall Stats");
        System.out.println("5: Leaderboards");
        System.out.println("6: Update Credentials");
        System.out.println("7: Logout");
        System.out.print("Choose option: ");

        String option = scanner.nextLine().trim();

        switch (option) {
            case "1" -> interactivePlayLoop(client, scanner);
            case "2" -> {
                fetchGameInfo(client);
                pauseForUser(scanner);
            }
            case "3" -> {
                fetchGameStats(client);
                pauseForUser(scanner);
            }
            case "4" -> {
                fetchPlayerStats(client);
                pauseForUser(scanner);
            }
            case "5" -> {
                fetchLeaderboard(client, scanner);
                pauseForUser(scanner);
            }
            case "6" -> {
                handleUpdateCredentials(client, scanner, username);
                pauseForUser(scanner);
            }
            case "7" -> {
                executeRequest(client, new Request.Logout("logout"), "Logged out successfully.", "Logout failed");
                pauseForUser(scanner);
                authLoop(client, scanner);
                return;
            }
            default -> {
                System.out.println("\n[!] Invalid selection.");
                pauseForUser(scanner);
            }
        }

        gameLoop(client, scanner, username);
    }

    private static void interactivePlayLoop(ClientMain client, Scanner scanner) {
        clearScreen();
        System.out.println("==================================================");
        System.out.println("  === PLAY MODE (Type 'back' to return to menu) ===");
        System.out.println("==================================================");
        fetchGameInfo(client);

        Predicate<String> processInput = input -> {
            if ("back".equalsIgnoreCase(input) || "exit".equalsIgnoreCase(input)) {
                return false;
            }

            List<String> words = Arrays.stream(input.split("[,\\s]+"))
                    .filter(s -> !s.isBlank())
                    .toList();

            clearScreen();
            System.out.println("==================================================");
            System.out.println("  === PLAY MODE (Type 'back' to return to menu) ===");
            System.out.println("==================================================");

            if (words.size() != 4) {
                System.out.println("\n[!] Format Error: Enter exactly 4 distinct words separated by spaces or commas.");
                return true;
            }

            Request req = new Request.SubmitProposal("submitProposal", words);
            executeRequest(client, req, "\n>>> Proposal Result", "\n>>> Proposal Rejected");
            System.out.println("\n--------------------------------------------------");
            fetchGameInfo(client);
            return true;
        };

        continuePlaySession(scanner, processInput);
    }

    private static void continuePlaySession(Scanner scanner, Predicate<String> processInput) {
        while (true) {
            System.out.print("\nEnter 4 words (or 'back'): ");
            String currentInput = scanner.nextLine().trim();

            if (!processInput.test(currentInput)) {
                break;
            }
        }
    }

    private static void fetchGameInfo(ClientMain client) {
        executeRequest(client, new Request.RequestGameInfo("gameInfo", null), "Current Game Info", "Could not fetch game info");
    }

    private static void fetchGameStats(ClientMain client) {
        executeRequest(client, new Request.RequestGameStats("gameStats", null), "Current Game Stats", "Could not fetch game stats");
    }

    private static void fetchPlayerStats(ClientMain client) {
        executeRequest(client, new Request.RequestPlayerStats("playerStats"), "Player Overall Stats", "Could not fetch player stats");
    }

    private static void fetchLeaderboard(ClientMain client, Scanner scanner) {
        System.out.print("Search specific player (press Enter for Top List): ");
        String targetPlayer = scanner.nextLine().trim();

        Request req = targetPlayer.isBlank()
                ? new Request.RequestLeaderboard("leaderboard", null, 10)
                : new Request.RequestLeaderboard("leaderboard", targetPlayer, null);

        executeRequest(client, req, "Leaderboard Info", "Could not retrieve leaderboard");
    }

    private static boolean handleUpdateCredentials(ClientMain client, Scanner scanner, String currentUsername) {
        clearScreen();
        System.out.println("--- Update Credentials ---");
        String targetUsername = currentUsername;

        if (targetUsername == null || targetUsername.isBlank()) {
            System.out.print("Username to update: ");
            targetUsername = scanner.nextLine().trim();
        }

        System.out.print("Current Password: ");
        String oldPsw = scanner.nextLine().trim();
        System.out.print("New Username (press Enter to keep current): ");
        String newUsernameInput = scanner.nextLine().trim();
        String newUsername = newUsernameInput.isBlank() ? targetUsername : newUsernameInput;
        System.out.print("New Password: ");
        String newPsw = scanner.nextLine().trim();

        Request req = new Request.UpdateCredentials("updateCredentials", targetUsername, oldPsw, newUsername, newPsw);
        return executeRequest(client, req, "Credentials Update", "Update Failed")
                .map(Response::success)
                .orElse(false);
    }

    private static Optional<Credentials> readCredentials(Scanner scanner, String action) {
        System.out.println("--- " + action + " ---");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        return Optional.of(new Credentials(username, password))
                .filter(c -> !c.username().isBlank() && !c.password().isBlank());
    }

    private static void pauseForUser(Scanner scanner) {
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }

    private static Optional<Response> executeRequest(ClientMain client, Request request, String successPrefix, String errorMsg) {
        try {
            Response response = client.sendRequest(request);
            printResponse(response, successPrefix, errorMsg);
            return Optional.of(response);
        } catch (IOException e) {
            System.err.println("Communication error: " + e.getMessage());
            return Optional.empty();
        }
    }

    private static void printResponse(Response response, String successPrefix, String errorMsg) {
        Optional.ofNullable(response)
                .ifPresentOrElse(
                        res -> Optional.of(res)
                                .filter(Response::success)
                                .ifPresentOrElse(
                                        s -> System.out.println(successPrefix + (s.result() != null ? ":\n" + s.result() : "")),
                                        () -> System.out.println(errorMsg + ": " + res.error())
                                ),
                        () -> System.out.println(errorMsg + ": No response from server")
                );
    }

    private record Credentials(String username, String password) {}
}