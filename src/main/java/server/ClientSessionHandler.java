package server;

import shared.JsonCodec;
import shared.Request;
import shared.Response;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public final class ClientSessionHandler implements Runnable {
    private final Socket socket;
    private final RequestDispatcher dispatcher;
    private String currentUser;

    public ClientSessionHandler(Socket socket, RequestDispatcher dispatcher) {
        this.socket = socket;
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        try (socket;
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             var writer = new PrintWriter(socket.getOutputStream(), true)) {
            reader.lines()
                    .filter(line -> !line.isBlank())
                    .map(this::processRequest)
                    .forEach(writer::println);
        } catch (Exception e) {
            System.err.println("Client session closed: " + e.getMessage());
        }
    }

    private String processRequest(String jsonLine) {
        try {
            Request request = JsonCodec.deserialize(jsonLine, Request.class);
            Response response = dispatcher.dispatch(request, currentUser);
            if (response.success()) {
                updateSessionState(request);
            }
            return JsonCodec.serialize(response);
        } catch (Exception e) {
            return JsonCodec.serializeError(e.getMessage());
        }
    }

    private void updateSessionState(Request request) {
        if (request instanceof Request.Login login) {
            currentUser = login.username();
        } else if (request instanceof Request.Logout) {
            currentUser = null;
        } else if (request instanceof Request.UpdateCredentials update
                && update.newUsername() != null
                && !update.newUsername().isBlank()) {
            currentUser = update.newUsername();
        }
    }
}