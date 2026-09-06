package server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import shared.dto.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public final class DummyServerMain {

    private static final int TCP_PORT = 8080;
    private static final long GAME_DURATION_MS = 60_000L;
    private static final int WORDS_PER_GROUP = 4;
    private static final int MAX_MISTAKES = 4;
    private static final int GROUPS_TO_WIN = 3;

    private static final List<List<String>> WORD_GROUPS = List.of(
            List.of("APPLE", "PEAR", "PLUM", "MANGO"),
            List.of("RED", "BLUE", "GREEN", "YELLOW"),
            List.of("CAT", "DOG", "MOUSE", "HORSE"),
            List.of("VIOLIN", "PIANO", "DRUM", "FLUTE")
    );
    private static final List<String> ALL_WORDS = WORD_GROUPS.stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toUnmodifiableList());

    private static final Gson GSON = new Gson();

    private final Map<String, String> passwords = new ConcurrentHashMap<>();
    private final Map<String, String> tokens = new ConcurrentHashMap<>();
    private final Map<String, Integer> udpPorts = new ConcurrentHashMap<>();
    private final Map<String, List<PlayerGuess>> guesses = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        new DummyServerMain().start();
    }

    private void start() throws Exception {
        System.out.println("Dummy Server starting...");
        System.out.println("TCP: 127.0.0.1:" + TCP_PORT);
        System.out.println("Game duration: " + (GAME_DURATION_MS / 1000) + " seconds");
        System.out.println("Try: register alice secret   then   login alice secret");

        Thread rollover = new Thread(this::gameLoop, "dummy-game-loop");
        rollover.setDaemon(true);
        rollover.start();

        try (ServerSocketChannel server = ServerSocketChannel.open()) {
            server.bind(new InetSocketAddress("127.0.0.1", TCP_PORT));
            while (true) {
                SocketChannel client = server.accept();
                Thread handler = new Thread(() -> handleClient(client), "dummy-client");
                handler.setDaemon(true);
                handler.start();
            }
        }
    }

    private void handleClient(SocketChannel channel) {
        try (SocketChannel client = channel;
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(client.socket().getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(client.socket().getOutputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    ApiRequest request = parseRequest(line);
                    ApiResponse<?> response = dispatch(request);
                    writer.write(GSON.toJson(response));
                    writer.newLine();
                    writer.flush();
                } catch (RuntimeException e) {
                    writer.write(GSON.toJson(new ApiResponse<>(false,
                            new ApiError(ErrorCode.INTERNAL_ERROR, "Server error: " + e.getMessage()), null)));
                    writer.newLine();
                    writer.flush();
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + e.getMessage());
        }
    }

    private ApiRequest parseRequest(String json) {
        JsonObject obj = GSON.fromJson(json, JsonObject.class);
        String operation = obj.get("operation").getAsString();
        return switch (operation) {
            case "register" -> GSON.fromJson(json, RegisterRequest.class);
            case "updateCredentials" -> GSON.fromJson(json, UpdateCredentialsRequest.class);
            case "login" -> GSON.fromJson(json, LoginRequest.class);
            case "logout" -> GSON.fromJson(json, LogoutRequest.class);
            case "submitProposal" -> GSON.fromJson(json, SubmitProposalRequest.class);
            case "requestGameInfo" -> GSON.fromJson(json, RequestGameInfoRequest.class);
            case "requestGameStats" -> GSON.fromJson(json, RequestGameStatsRequest.class);
            case "requestLeaderboard" -> GSON.fromJson(json, RequestLeaderboardRequest.class);
            case "requestPlayerStats" -> GSON.fromJson(json, RequestPlayerStatsRequest.class);
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        };
    }

    private ApiResponse<?> dispatch(ApiRequest request) {
        try {
            return switch (request) {
                case RegisterRequest req -> ok(register(req));
                case UpdateCredentialsRequest req -> ok(update(req));
                case LoginRequest req -> ok(login(req));
                case LogoutRequest req -> ok(logout(req));
                case SubmitProposalRequest req -> ok(submit(req));
                case RequestGameInfoRequest req -> ok(gameInfo(req));
                case RequestGameStatsRequest req -> ok(gameStats(req));
                case RequestLeaderboardRequest req -> ok(leaderboard(req));
                case RequestPlayerStatsRequest req -> ok(playerStats(req));
                default -> fail(ErrorCode.INTERNAL_ERROR, "Unsupported operation");
            };
        } catch (IllegalArgumentException e) {
            return fail(ErrorCode.INTERNAL_ERROR, e.getMessage());
        } catch (Exception e) {
            return fail(ErrorCode.INTERNAL_ERROR, "Server error: " + e.getMessage());
        }
    }

    // ----- Account operations -----
    private ApiResponse<RegisterData> register(RegisterRequest request) {
        if (passwords.putIfAbsent(request.username(), request.psw()) != null) {
            return fail(ErrorCode.USERNAME_ALREADY_REGISTERED, "Username already registered");
        }
        return ok(new RegisterData(request.username()));
    }

    private ApiResponse<UpdateCredentialsData> update(UpdateCredentialsRequest request) {
        String oldPass = passwords.get(request.oldUsername());
        if (oldPass == null || !oldPass.equals(request.oldPsw())) {
            return fail(ErrorCode.INCORRECT_PASSWORD, "Incorrect password");
        }
        if (!request.oldUsername().equals(request.newUsername()) && passwords.containsKey(request.newUsername())) {
            return fail(ErrorCode.NEW_USERNAME_ALREADY_TAKEN, "Username already taken");
        }
        passwords.remove(request.oldUsername());
        passwords.put(request.newUsername(), request.newPsw());
        tokens.entrySet().removeIf(e -> e.getValue().equals(request.oldUsername()));
        return ok(new UpdateCredentialsData(request.newUsername()));
    }

    private ApiResponse<LoginData> login(LoginRequest request) {
        String storedPass = passwords.get(request.username());
        if (storedPass == null || !storedPass.equals(request.psw())) {
            return fail(ErrorCode.INCORRECT_PASSWORD, "Incorrect password");
        }
        String token = "dummy-" + UUID.randomUUID();
        tokens.put(token, request.username());
        udpPorts.put(request.username(), request.udpPort());
        getOrCreateGuesses(request.username());
        return ok(new LoginData(token));
    }

    private ApiResponse<LogoutData> logout(LogoutRequest request) {
        String username = tokens.remove(request.accountToken());
        if (username == null) {
            return fail(ErrorCode.USER_NOT_LOGGED_IN, "Invalid token");
        }
        udpPorts.remove(username);
        return ok(new LogoutData());
    }

    // ----- Game operations -----
    private ApiResponse<GameInfoData> submit(SubmitProposalRequest request) {
        String username = resolveToken(request.accountToken());
        if (username == null) return fail(ErrorCode.USER_NOT_LOGGED_IN, "Invalid token");

        List<String> words = request.words();
        if (words.size() != WORDS_PER_GROUP || new HashSet<>(words).size() != WORDS_PER_GROUP) {
            return fail(ErrorCode.MALFORMED_PROPOSAL,
                    "Proposal must contain exactly " + WORDS_PER_GROUP + " distinct words");
        }
        if (!ALL_WORDS.containsAll(words)) {
            return fail(ErrorCode.UNKNOWN_WORDS_IN_PROPOSAL, "Unknown word");
        }

        List<PlayerGuess> playerGuesses = getOrCreateGuesses(username);
        Set<String> alreadyGrouped = playerGuesses.stream()
                .flatMap(g -> g.words().stream())
                .collect(Collectors.toSet());
        if (!Collections.disjoint(alreadyGrouped, words)) {
            return fail(ErrorCode.WORDS_ALREADY_GROUPED, "One or more words already grouped");
        }

        boolean correct = WORD_GROUPS.stream()
                .map(HashSet::new)
                .anyMatch(g -> g.equals(new HashSet<>(words)));
        playerGuesses.add(new PlayerGuess(currentGameId(), List.copyOf(words), correct));

        return ok(buildGameInfo(username, currentGameId(), false));
    }

    private ApiResponse<GameInfoData> gameInfo(RequestGameInfoRequest request) {
        String username = resolveToken(request.accountToken());
        if (username == null) return fail(ErrorCode.USER_NOT_LOGGED_IN, "Invalid token");

        long gameId = request.gameId() == null ? currentGameId() : request.gameId();
        if (gameId < 0 || gameId > currentGameId()) {
            return fail(ErrorCode.GAME_NOT_FOUND, "Game not found");
        }
        getOrCreateGuesses(username);
        return ok(buildGameInfo(username, gameId, gameId < currentGameId()));
    }

    private ApiResponse<GameStatsData> gameStats(RequestGameStatsRequest request) {
        String username = resolveToken(request.accountToken());
        if (username == null) return fail(ErrorCode.USER_NOT_LOGGED_IN, "Invalid token");

        long gameId = request.gameId() == null ? currentGameId() : request.gameId();
        if (gameId < 0 || gameId > currentGameId()) {
            return fail(ErrorCode.GAME_NOT_FOUND, "Game not found");
        }

        List<String> participants = computeParticipants(gameId);
        int completed = 0, winners = 0, totalScore = 0;

        for (String player : participants) {
            GameInfoData info = buildGameInfo(player, gameId, gameId < currentGameId());
            int score = calculateScore(info);
            totalScore += score;
            String status = determineStatus(info);
            if (!"ACTIVE".equals(status)) completed++;
            if ("WON".equals(status)) winners++;
        }

        boolean isCompleted = gameId < currentGameId();
        int active = isCompleted ? 0 : Math.max(0, participants.size() - completed);
        double avgScore = participants.isEmpty() ? 0.0 : (double) totalScore / participants.size();

        return ok(new GameStatsData(
                gameId,
                isCompleted,
                expiresAt(gameId),
                participants.size(),
                active,
                completed,
                winners,
                avgScore
        ));
    }

    private ApiResponse<LeaderboardData> leaderboard(RequestLeaderboardRequest request) {
        String username = resolveToken(request.accountToken());
        if (username == null) return fail(ErrorCode.USER_NOT_LOGGED_IN, "Invalid token");

        List<LeaderboardEntry> allEntries = passwords.keySet().stream()
                .map(name -> new LeaderboardEntry(name, totalScore(name), 0))
                .sorted(Comparator.comparingInt(LeaderboardEntry::score).reversed()
                        .thenComparing(LeaderboardEntry::username))
                .toList();

        List<LeaderboardEntry> ranked = new ArrayList<>();
        int rank = 1;
        for (LeaderboardEntry e : allEntries) {
            ranked.add(new LeaderboardEntry(e.username(), e.score(), rank++));
        }

        List<LeaderboardEntry> top = request.topPlayers() == null
                ? ranked
                : ranked.subList(0, Math.min(request.topPlayers(), ranked.size()));

        LeaderboardEntry requested = request.playerName() == null
                ? null
                : ranked.stream()
                .filter(e -> e.username().equals(request.playerName()))
                .findFirst()
                .orElse(null);

        return ok(new LeaderboardData(top, requested, ranked.size()));
    }

    private ApiResponse<PlayerStatsData> playerStats(RequestPlayerStatsRequest request) {
        String username = resolveToken(request.accountToken());
        if (username == null) return fail(ErrorCode.USER_NOT_LOGGED_IN, "Invalid token");

        Map<Integer, Integer> histogram = new LinkedHashMap<>();
        int completed = 0, wins = 0, losses = 0, perfect = 0;
        List<Long> finishedGames = new ArrayList<>();

        for (long game = 0; game < currentGameId(); game++) {
            GameInfoData info = buildGameInfo(username, game, true);
            if (info.correctGuesses().isEmpty() && info.wrongGuesses().isEmpty()) {
                continue;
            }
            String status = determineStatus(info);
            if (!"ACTIVE".equals(status)) {
                completed++;
                if ("WON".equals(status)) wins++;
                else if ("LOST".equals(status)) losses++;
                int mistakes = info.wrongGuesses().size();
                histogram.merge(mistakes, 1, Integer::sum);
                if ("WON".equals(status) && mistakes == 0) perfect++;
                finishedGames.add(game);
            }
        }

        int currentStreak = 0;
        for (int i = finishedGames.size() - 1; i >= 0; i--) {
            GameInfoData info = buildGameInfo(username, finishedGames.get(i), true);
            if ("WON".equals(determineStatus(info))) {
                currentStreak++;
            } else {
                break;
            }
        }
        int maxStreak = currentStreak;

        if (currentGameId() == 0 && completed == 0) {
            histogram.putIfAbsent(0, 0);
        }

        double winRate = completed == 0 ? 0 : (100.0 * wins / completed);
        double lossRate = completed == 0 ? 0 : (100.0 * losses / completed);

        return ok(new PlayerStatsData(
                completed,
                winRate,
                lossRate,
                currentStreak,
                maxStreak,
                perfect,
                histogram
        ));
    }

    // ----- Game info builder -----
    private GameInfoData buildGameInfo(String username, long gameId, boolean expired) {
        List<PlayerGuess> playerGuesses = guesses.getOrDefault(key(username, gameId), List.of());
        List<Set<String>> correct = playerGuesses.stream()
                .filter(PlayerGuess::correct)
                .map(PlayerGuess::words)
                .map(HashSet::new)
                .map(Set::copyOf)
                .toList();
        List<Set<String>> wrong = playerGuesses.stream()
                .filter(g -> !g.correct())
                .map(PlayerGuess::words)
                .map(HashSet::new)
                .map(Set::copyOf)
                .toList();

        List<List<String>> correctGroups = expired ? WORD_GROUPS : null;

        List<String> shuffledWords = new ArrayList<>(ALL_WORDS);
        Collections.shuffle(shuffledWords, new Random(gameId));

        return new GameInfoData(
                gameId,
                expiresAt(gameId),
                shuffledWords,
                correct,
                wrong,
                correctGroups
        );
    }

    // ----- Helper methods -----
    private List<String> computeParticipants(long gameId) {
        List<String> participants = new ArrayList<>();
        for (Map.Entry<String, List<PlayerGuess>> entry : guesses.entrySet()) {
            String username = entry.getKey();
            List<PlayerGuess> list = entry.getValue();
            boolean participated = list.stream().anyMatch(g -> g.gameId() == gameId);
            if (participated || (gameId == currentGameId() && tokens.containsValue(username))) {
                participants.add(username);
            }
        }
        return participants;
    }

    private int calculateScore(GameInfoData info) {
        return info.correctGuesses().size() * 6 - info.wrongGuesses().size() * 4;
    }

    private String determineStatus(GameInfoData info) {
        if (info.correctGuesses().size() >= GROUPS_TO_WIN) return "WON";
        if (info.wrongGuesses().size() >= MAX_MISTAKES) return "LOST";
        if (info.expiresAt() <= System.currentTimeMillis()) return "INCOMPLETE";
        return "ACTIVE";
    }

    private int totalScore(String username) {
        int total = 0;
        for (long game = 0; game <= currentGameId(); game++) {
            GameInfoData info = buildGameInfo(username, game, game < currentGameId());
            total += calculateScore(info);
        }
        return total;
    }

    private long currentGameId() {
        return System.currentTimeMillis() / GAME_DURATION_MS;
    }

    private long expiresAt(long gameId) {
        return (gameId + 1) * GAME_DURATION_MS;
    }

    private List<PlayerGuess> getOrCreateGuesses(String username) {
        return guesses.computeIfAbsent(key(username, currentGameId()),
                k -> new CopyOnWriteArrayList<>());
    }

    private static String key(String username, long gameId) {
        return username + "@" + gameId;
    }

    private String resolveToken(String token) {
        return tokens.get(token);
    }

    // ----- Game rollover and UDP notifications -----
    private void gameLoop() {
        long lastGame = currentGameId();
        while (true) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                return;
            }
            long now = currentGameId();
            if (now != lastGame) {
                announceGameEnd(lastGame);
                lastGame = now;
            }
        }
    }

    private void announceGameEnd(long gameId) {
        System.out.println("Game " + gameId + " ended. Sending UDP notifications.");
        for (Map.Entry<String, Integer> entry : udpPorts.entrySet()) {
            String username = entry.getKey();
            int port = entry.getValue();
            if (port <= 0) continue;

            GameInfoData result = buildGameInfo(username, gameId, true);
            String json = GSON.toJson(result);
            byte[] payload = json.getBytes(StandardCharsets.UTF_8);

            try (DatagramSocket socket = new DatagramSocket()) {
                DatagramPacket packet = new DatagramPacket(
                        payload, payload.length,
                        InetAddress.getLoopbackAddress(), port
                );
                socket.send(packet);
            } catch (IOException e) {
                System.out.println("Failed to notify " + username + ": " + e.getMessage());
            }
        }
    }

    // ----- Utility responses -----
    private static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, data);
    }

    private static <T> ApiResponse<T> fail(ErrorCode code, String message) {
        return new ApiResponse<>(false, new ApiError(code, message), null);
    }

    private record PlayerGuess(long gameId, List<String> words, boolean correct) {}
}
