package client;

public final class ClientContext {
    private final NetworkSession session;
    private final ConsoleView view;
    private final GameObserver observer;
    private ClientState state;
    private boolean exitRequested = false;

    public ClientContext(NetworkSession session, ConsoleView view, GameObserver observer) {
        this.session = session;
        this.view = view;
        this.observer = observer;
    }

    public NetworkSession getSession() { return session; }
    public ConsoleView getView() { return view; }
    public GameObserver getObserver() { return observer; }

    public ClientState getState() { return state; }
    public void setState(ClientState newState) { this.state = newState; }

    public boolean isExitRequested() { return exitRequested; }
    public void requestExit() { this.exitRequested = true; }
}