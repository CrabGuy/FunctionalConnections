package server.network;
public interface GameTransitionWatcher extends Runnable {
    void startWatching();
    void stopWatching();
}
