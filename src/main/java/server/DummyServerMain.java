package server;

import client.json.JsonWriter;
import client.json.ProtocolCodec;
import shared.dto.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small in-memory server for exercising the client without the real server implementation.
 * It supports every client operation and sends UDP game-end notifications on each game rollover.
 */
public final class DummyServerMain {
    private static final int TCP_PORT = 5000;
    private static final long GAME_DURATION_MS = 60_000L;

    private static final List<List<String>> GROUPS = List.of(
            List.of("APPLE", "PEAR", "PLUM", "MANGO"),
            List.of("RED", "BLUE", "GREEN", "YELLOW"),
            List.of("CAT", "DOG", "MOUSE", "HORSE"),
            List.of("VIOLIN", "PIANO", "DRUM", "FLUTE")
    );
    private static final List<String> WORDS = GROUPS.stream().flatMap(Collection::stream).toList();

    private final Map<String, String> passwords = new ConcurrentHashMap<>();
    private final Map<String, String> tokens = new ConcurrentHashMap<>();
    private final Map<String, Integer> udpPorts = new ConcurrentHashMap<>();
    private final Map<String, List<PlayerGuess>> guesses = new ConcurrentHashMap<>();
    private final Random random = new Random(7);

    private DummyServerMain() {
    }

    public static void main(String[] args) throws Exception {
        DummyServerMain server = new DummyServerMain();
        server.start();
    }

    private void start() throws Exception {
        System.out.println("Dummy Connections server");
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
             BufferedReader reader = new BufferedReader(new InputStreamReader(client.socket().getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.socket().getOutputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    ApiRequest request = ProtocolCodec.requestFromJson(line);
                    ApiResponse<?> response = dispatch(request);
                    writer.write(responseJson(response));
                    writer.newLine();
                    writer.flush();
                } catch (RuntimeException e) {
                    writer.write(errorJson(ErrorCode.INTERNAL_ERROR, "Dummy server error: " + e.getMessage()));
                    writer.newLine();
                    writer.flush();
                }
            }
        } catch (IOException e) {
            System.out.println("Client connection closed: " + e.getMessage());
        }
    }

    private ApiResponse<?> dispatch(ApiRequest request) {
        return switch (request.getOperation()) {
            case "register" -> register((RegisterRequest) request);
            case "updateCredentials" -> update((UpdateCredentialsRequest) request);
            case "login" -> login((LoginRequest) request);
            case "logout" -> logout((LogoutRequest) request);
            case "submitProposal" -> submit((SubmitProposalRequest) request);
            case "requestGameInfo" -> gameInfo((RequestGameInfoRequest) request);
            case "requestGameStats" -> gameStats((RequestGameStatsRequest) request);
            case "requestLeaderboard" -> leaderboard((RequestLeaderboardRequest) request);
            case "requestPlayerStats" -> playerStats((RequestPlayerStatsRequest) request);
            default -> new ApiResponse<>(false, new ApiError(ErrorCode.INTERNAL_ERROR, "Unknown operation"), null);
        };
    }

    private ApiResponse<RegisterData> register(RegisterRequest request) {
        if (passwords.putIfAbsent(request.username(), request.password()) != null) {
            return fail(ErrorCode.USERNAME_ALREADY_REGISTERED, "Username already registered");
        }
        return ok(new RegisterData(request.username()));
    }

    private ApiResponse<UpdateCredentialsData> update(UpdateCredentialsRequest request) {
        String oldPassword = passwords.get(request.oldUsername());
        if (oldPassword == null || !Objects.equals(oldPassword, request.oldPassword())) {
            return fail(ErrorCode.INCORRECT_PASSWORD, "Incorrect password");
        }
        if (!request.oldUsername().equals(request.newUsername()) && passwords.containsKey(request.newUsername())) {
            return fail(ErrorCode.NEW_USERNAME_ALREADY_TAKEN, "Username already taken");
        }
        passwords.remove(request.oldUsername());
        passwords.put(request.newUsername(), request.newPassword());
        tokens.entrySet().removeIf(e -> e.getValue().equals(request.oldUsername()));
        return ok(new UpdateCredentialsData(request.newUsername()));
    }

    private ApiResponse<LoginData> login(LoginRequest request) {
        String password = passwords.get(request.username());
        if (password == null || !Objects.equals(password, request.password())) {
            return fail(ErrorCode.INCORRECT_PASSWORD, "Incorrect password");
        }
        String token = "dummy-" + UUID.randomUUID();
        tokens.put(token, request.username());
        udpPorts.put(request.username(), request.udpPort());
        currentGuesses(request.username());
        return ok(new LoginData(token));
    }

    private ApiResponse<LogoutData> logout(LogoutRequest request) {
        String username = tokens.remove(request.accountToken());
        if (username == null) {
            return fail(ErrorCode.USER_NOT_LOGGED_IN, "Invalid account token");
        }
        udpPorts.remove(username);
        return ok(new LogoutData());
    }

    private ApiResponse<GameInfoData> submit(SubmitProposalRequest request) {
        String username = resolve(request.accountToken());
        if (username == null) return fail(ErrorCode.USER_NOT_LOGGED_IN, "Invalid account token");
        List<String> words = request.words();
        if (words.size() != 4 || new HashSet<>(words).size() != 4) {
            return fail(ErrorCode.MALFORMED_PROPOSAL, "Proposal must contain exactly 4 distinct words");
        }
        if (!WORDS.containsAll(words)) return fail(ErrorCode.UNKNOWN_WORDS_IN_PROPOSAL, "Unknown word");

        List<PlayerGuess> playerGuesses = currentGuesses(username);
        Set<String> already = playerGuesses.stream().flatMap(g -> g.words().stream()).collect(java.util.stream.Collectors.toSet());
        if (!Collections.disjoint(already, words)) {
            return fail(ErrorCode.WORDS_ALREADY_GROUPED, "One or more words have already been grouped");
        }
        boolean correct = GROUPS.stream().map(HashSet::new).anyMatch(g -> g.equals(new HashSet<>(words)));
        playerGuesses.add(new PlayerGuess(currentGameId(), List.copyOf(words), correct));
        return ok(buildGameInfo(username, currentGameId(), false));
    }

    private ApiResponse<GameInfoData> gameInfo(RequestGameInfoRequest request) {
        String username = resolve(request.accountToken());
        if (username == null) return fail(ErrorCode.USER_NOT_LOGGED_IN, "Invalid account token");
        long gameId = request.gameId() == null ? currentGameId() : request.gameId();
        if (gameId < 0 || gameId > currentGameId()) return fail(ErrorCode.GAME_NOT_FOUND, "Game not found");
        currentGuesses(username);
        return ok(buildGameInfo(username, gameId, gameId < currentGameId()));
    }

    private ApiResponse<GameStatsData> gameStats(RequestGameStatsRequest request) {
        String username = resolve(request.accountToken());
        if (username == null) return fail(ErrorCode.USER_NOT_LOGGED_IN, "Invalid account token");
        long gameId = request.gameId() == null ? currentGameId() : request.gameId();
        if (gameId < 0 || gameId > currentGameId()) return fail(ErrorCode.GAME_NOT_FOUND, "Game not found");
        List<String> participants = new ArrayList<>();
        for (Map.Entry<String, List<PlayerGuess>> e : guesses.entrySet()) {
            if (e.getValue().stream().anyMatch(g -> g.gameId() == gameId) || (gameId == currentGameId() && tokens.containsValue(e.getKey()))) {
                participants.add(e.getKey());
            }
        }
        int completed = 0;
        int winners = 0;
        int scoreTotal = 0;
        for (String player : participants) {
            GameInfoData info = buildGameInfo(player, gameId, gameId < currentGameId());
            int score = score(info);
            scoreTotal += score;
            String status = status(info);
            if (!"ACTIVE".equals(status)) completed++;
            if ("WON".equals(status)) winners++;
        }
        boolean done = gameId < currentGameId();
        return ok(new GameStatsData(gameId, done, expiresAt(gameId), participants.size(), done ? 0 : Math.max(0, participants.size() - completed), completed, winners,
                participants.isEmpty() ? 0.0 : (double) scoreTotal / participants.size()));
    }

    private ApiResponse<LeaderboardData> leaderboard(RequestLeaderboardRequest request) {
        String username = resolve(request.accountToken());
        if (username == null) return fail(ErrorCode.USER_NOT_LOGGED_IN, "Invalid account token");
        List<LeaderboardEntry> all = passwords.keySet().stream()
                .map(name -> new LeaderboardEntry(name, totalScore(name), 0))
                .sorted(Comparator.comparingInt(LeaderboardEntry::score).reversed().thenComparing(LeaderboardEntry::username))
                .toList();
        List<LeaderboardEntry> ranked = new ArrayList<>();
        int rank = 1;
        for (LeaderboardEntry e : all) ranked.add(new LeaderboardEntry(e.username(), e.score(), rank++));
        List<LeaderboardEntry> top = request.topPlayers() == null ? ranked : ranked.subList(0, Math.min(request.topPlayers(), ranked.size()));
        LeaderboardEntry requested = request.playerName() == null ? null : ranked.stream().filter(e -> e.username().equals(request.playerName())).findFirst().orElse(null);
        return ok(new LeaderboardData(top, requested, ranked.size()));
    }

    private ApiResponse<PlayerStatsData> playerStats(RequestPlayerStatsRequest request) {
        String username = resolve(request.accountToken());
        if (username == null) return fail(ErrorCode.USER_NOT_LOGGED_IN, "Invalid account token");
        Map<Integer, Integer> histogram = new LinkedHashMap<>();
        int completed = 0, wins = 0, losses = 0, currentStreak = 0, maxStreak = 0, perfect = 0;
        List<Long> finishedGames = new ArrayList<>();
        for (long game = 0; game < currentGameId(); game++) {
            GameInfoData info = buildGameInfo(username, game, true);
            if (info.correctGuesses().isEmpty() && info.wrongGuesses().isEmpty()) continue;
            String status = status(info);
            if ("WON".equals(status) || "LOST".equals(status) || "INCOMPLETE".equals(status)) {
                completed++;
                if ("WON".equals(status)) wins++; else if ("LOST".equals(status)) losses++;
                int mistakes = info.wrongGuesses().size();
                histogram.merge(mistakes, 1, Integer::sum);
                if ("WON".equals(status) && mistakes == 0) perfect++;
                finishedGames.add(game);
            }
        }
        // Keep the current streak calculation intentionally simple for the dummy server.
        for (int i = finishedGames.size() - 1; i >= 0; i--) {
            GameInfoData info = buildGameInfo(username, finishedGames.get(i), true);
            if (!"WON".equals(status(info))) break;
            currentStreak++;
        }
        maxStreak = currentStreak;
        if (currentGameId() == 0 && completed == 0) histogram.putIfAbsent(0, 0);
        double winRate = completed == 0 ? 0 : (100.0 * wins / completed);
        double lossRate = completed == 0 ? 0 : (100.0 * losses / completed);
        return ok(new PlayerStatsData(completed, winRate, lossRate, currentStreak, maxStreak, perfect, histogram));
    }

    private GameInfoData buildGameInfo(String username, long gameId, boolean expired) {
        List<PlayerGuess> playerGuesses = guesses.getOrDefault(key(username, gameId), List.of());
        List<Set<String>> correct = playerGuesses.stream().filter(PlayerGuess::correct).map(PlayerGuess::words).map(HashSet::new).map(Set::copyOf).toList();
        List<Set<String>> wrong = playerGuesses.stream().filter(g -> !g.correct()).map(PlayerGuess::words).map(HashSet::new).map(Set::copyOf).toList();
        List<List<String>> groups = expired ? GROUPS : null;
        List<String> shuffled = new ArrayList<>(WORDS);
        Collections.shuffle(shuffled, new Random(gameId));
        return new GameInfoData(gameId, expiresAt(gameId), shuffled, correct, wrong, groups);
    }

    private int score(GameInfoData info) { return info.correctGuesses().size() * 6 - info.wrongGuesses().size() * 4; }

    private String status(GameInfoData info) {
        if (info.correctGuesses().size() >= 3) return "WON";
        if (info.wrongGuesses().size() >= 4) return "LOST";
        return info.expiresAt() <= System.currentTimeMillis() ? "INCOMPLETE" : "ACTIVE";
    }

    private int totalScore(String username) {
        int score = 0;
        for (long game = 0; game < currentGameId(); game++) {
            score += score(buildGameInfo(username, game, true));
        }
        score += score(buildGameInfo(username, currentGameId(), false));
        return score;
    }

    private long currentGameId() { return System.currentTimeMillis() / GAME_DURATION_MS; }
    private long expiresAt(long gameId) { return (gameId + 1) * GAME_DURATION_MS; }

    private List<PlayerGuess> currentGuesses(String username) {
        return guesses.computeIfAbsent(key(username, currentGameId()), ignored -> Collections.synchronizedList(new ArrayList<>()));
    }

    private static String key(String username, long gameId) { return username + "@" + gameId; }
    private String resolve(String token) { return tokens.get(token); }

    private void gameLoop() {
        long last = currentGameId();
        while (true) {
            try { Thread.sleep(250); } catch (InterruptedException e) { return; }
            long now = currentGameId();
            if (now != last) {
                announceEnd(last);
                last = now;
            }
        }
    }

    private void announceEnd(long gameId) {
        System.out.println("Game " + gameId + " ended; sending UDP notifications.");
        for (String username : udpPorts.keySet()) {
            int port = udpPorts.getOrDefault(username, -1);
            if (port <= 0) continue;
            GameInfoData result = buildGameInfo(username, gameId, true);
            byte[] payload = JsonWriter.write(gameInfoMap(result)).getBytes(StandardCharsets.UTF_8);
            try (DatagramSocket socket = new DatagramSocket()) {
                DatagramPacket packet = new DatagramPacket(payload, payload.length, InetAddress.getLoopbackAddress(), port);
                socket.send(packet);
            } catch (IOException e) {
                System.out.println("Could not notify " + username + ": " + e.getMessage());
            }
        }
    }

    private static String responseJson(ApiResponse<?> response) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("success", response.success());
        root.put("error", response.error() == null ? null : Map.of("code", response.error().code().name(), "message", response.error().message()));
        root.put("data", response.data() == null ? null : dataMap(response.data()));
        return JsonWriter.write(root);
    }

    private static String errorJson(ErrorCode code, String message) {
        return responseJson(new ApiResponse<>(false, new ApiError(code, message), null));
    }

    private static Map<String, Object> dataMap(Object data) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (data instanceof RegisterData d) { map.put("username", d.username()); }
        else if (data instanceof LoginData d) { map.put("accountToken", d.accountToken()); }
        else if (data instanceof LogoutData) { }
        else if (data instanceof UpdateCredentialsData d) { map.put("newUsername", d.newUsername()); }
        else if (data instanceof GameInfoData d) { map.putAll(gameInfoMap(d)); }
        else if (data instanceof GameStatsData d) {
            map.put("gameId", d.gameId()); map.put("completed", d.completed()); map.put("expiresAt", d.expiresAt());
            map.put("totalParticipants", d.totalParticipants()); map.put("activePlayers", d.activePlayers());
            map.put("completedPlayers", d.completedPlayers()); map.put("winners", d.winners()); map.put("averageScore", d.averageScore());
        } else if (data instanceof LeaderboardData d) {
            map.put("topPlayers", d.topPlayers().stream().map(DummyServerMain::leaderboardEntryMap).toList());
            map.put("requestedPlayer", d.requestedPlayer() == null ? null : leaderboardEntryMap(d.requestedPlayer()));
            map.put("totalPlayers", d.totalPlayers());
        } else if (data instanceof PlayerStatsData d) {
            map.put("puzzlesCompleted", d.puzzlesCompleted()); map.put("winRate", d.winRate()); map.put("lossRate", d.lossRate());
            map.put("currentStreak", d.currentStreak()); map.put("maxStreak", d.maxStreak()); map.put("perfectPuzzles", d.perfectPuzzles());
            Map<String, Object> histogram = new LinkedHashMap<>();
            d.mistakeHistogram().forEach((k,v) -> histogram.put(String.valueOf(k), v));
            map.put("mistakeHistogram", histogram);
        } else throw new IllegalArgumentException("Unsupported data: " + data);
        return map;
    }

    private static Map<String, Object> leaderboardEntryMap(LeaderboardEntry e) {
        return Map.of("username", e.username(), "score", e.score(), "rank", e.rank());
    }

    private static Map<String, Object> gameInfoMap(GameInfoData d) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("gameId", d.gameId()); map.put("expiresAt", d.expiresAt()); map.put("words", d.words());
        map.put("correctGuesses", d.correctGuesses()); map.put("wrongGuesses", d.wrongGuesses()); map.put("correctGroups", d.correctGroups());
        return map;
    }

    private static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(true, null, data); }
    private static <T> ApiResponse<T> fail(ErrorCode code, String message) { return new ApiResponse<>(false, new ApiError(code, message), null); }

    private record PlayerGuess(long gameId, List<String> words, boolean correct) {}
}
