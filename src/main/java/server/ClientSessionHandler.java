package server;

import server.handlers.RequestDispatcher;
import shared.Request;
import shared.Response;
import shared.JsonCodec;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

public final class ClientSessionHandler implements Runnable {
    private final Socket socket;
    private final RequestDispatcher dispatcher;
    private final ServiceContext serviceContext;

    public ClientSessionHandler(Socket socket, RequestDispatcher dispatcher, ServiceContext serviceContext) {
        this.socket = socket;
        this.dispatcher = dispatcher;
        this.serviceContext = serviceContext;
    }

    @Override
    public void run() {
        AtomicReference<String> currentUser = new AtomicReference<>();

        try (socket;
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             var writer = new PrintWriter(socket.getOutputStream(), true)) {
            reader.lines()
                    .filter(line -> !line.isBlank())
                    .map(line -> processRequest(line, currentUser))
                    .forEach(writer::println);
        } catch (Exception e) {
            System.err.println("Client session closed: " + e.getMessage());
        }
    }

    private String processRequest(String jsonLine, AtomicReference<String> currentUser) {
        try {
            Request request = JsonCodec.deserialize(jsonLine, Request.class);
            Response response = dispatcher.dispatch(request, serviceContext, currentUser.get());

            if (response.success()) {
                updateSessionState(request, currentUser);
            }

            return JsonCodec.serialize(response);
        } catch (Exception e) {
            return JsonCodec.serializeError(e.getMessage());
        }
    }

    private void updateSessionState(Request request, AtomicReference<String> currentUser) {
        if (request instanceof Request.Login login) {
            currentUser.set(login.username());
        } else if (request instanceof Request.Logout) {
            currentUser.set(null);
        } else if (request instanceof Request.UpdateCredentials update
                && update.newUsername() != null
                && !update.newUsername().isBlank()) {
            currentUser.set(update.newUsername());
        }
    }
}