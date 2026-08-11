package client;

import shared.JsonCodec;
import shared.Request;
import shared.Response;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ClientMain implements AutoCloseable {
    private final SocketChannel socketChannel;

    public ClientMain(String host, int port) throws IOException {
        this.socketChannel = SocketChannel.open(new InetSocketAddress(host, port));
    }

    public Response sendRequest(Request request) throws IOException {
        String payload = JsonCodec.serialize(request) + "\n";
        socketChannel.write(ByteBuffer.wrap(payload.getBytes(StandardCharsets.UTF_8)));

        ByteBuffer buffer = ByteBuffer.allocate(1024);
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
        if (socketChannel != null) {
            socketChannel.close();
        }
    }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in);
             ClientMain client = new ClientMain("localhost", 8080)) {

            System.out.println("=== CONNECTIONS CLIENT ===");
            System.out.print("Choose action [1: Register, 2: Login]: ");
            String choice = scanner.nextLine().trim();

            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            System.out.print("Password: ");
            String password = scanner.nextLine().trim();

            boolean authenticated = switch (choice) {
                case "1" -> registerAndLogin(client, username, password);
                case "2" -> login(client, username, password);
                default -> {
                    System.out.println("Invalid option.");
                    yield false;
                }
            };

            if (authenticated) {
                fetchGameInfo(client);
            }

        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    private static boolean registerAndLogin(ClientMain client, String username, String password) throws IOException {
        Response regResponse = client.sendRequest(new Request.Register("register", username, password));
        printResponse(regResponse, "Registration successful!", "Registration failed");
        return regResponse.success() && login(client, username, password);
    }

    private static boolean login(ClientMain client, String username, String password) throws IOException {
        Response loginResponse = client.sendRequest(new Request.Login("login", username, password));
        printResponse(loginResponse, "Login successful!", "Login failed");
        return loginResponse.success();
    }

    private static void fetchGameInfo(ClientMain client) throws IOException {
        Response gameInfoResp = client.sendRequest(new Request.RequestGameInfo("requestGameInfo", null));
        printResponse(gameInfoResp, "Current Game Info: " + gameInfoResp.result(), "Could not retrieve game info");
    }

    private static void printResponse(Response response, String successMsg, String errorMsg) {
        if (response.success()) {
            System.out.println(successMsg);
        } else {
            System.out.println(errorMsg + ": " + response.error());
        }
    }
}