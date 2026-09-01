package client;

import java.util.Scanner;

import client.states.AuthState;

public final class GameLoopController {
    private final NetworkSession session;
    private final ConsoleView view;
    private final GameObserver observer;

    public GameLoopController(NetworkSession session, ConsoleView view, GameObserver observer) {
        this.session = session;
        this.view = view;
        this.observer = observer;
    }

    public void run(Scanner scanner) {
        ClientContext context = new ClientContext(session, view, observer);
        context.setState(new AuthState());
        while (!context.isExitRequested()) {
            context.getState().handle(scanner, context);
        }
    }
}