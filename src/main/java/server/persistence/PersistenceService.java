package server.persistence;
import server.account.AccountRepository;
import server.game.PlayerGameRepository;
public interface PersistenceService {
    void saveSnapshot(AccountRepository accounts, PlayerGameRepository playerGames) throws java.io.IOException;
    void loadSnapshot(AccountRepository accounts, PlayerGameRepository playerGames) throws java.io.IOException;
    void schedulePeriodicSnapshot(AccountRepository accounts, PlayerGameRepository playerGames);
    void shutdown();
}
