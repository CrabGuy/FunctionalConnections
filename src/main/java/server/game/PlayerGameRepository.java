package server.game;

import server.dto.PlayerGame;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PlayerGameRepository {
    PlayerGame findOrCreate(String username, long gameId);
    void save(PlayerGame playerGame);
    List<PlayerGame> findByGame(long gameId);
    List<PlayerGame> findPlayerGameByUsername(String username);
    Set<String> findAllUsernames();
    Optional<PlayerGame> findByUsernameAndGame(String username, long gameId);
    List<PlayerGame> findAll();
}