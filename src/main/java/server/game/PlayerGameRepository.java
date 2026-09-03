package server.game;

import server.dto.PlayerGame;

import java.util.List;
import java.util.Optional;

public interface PlayerGameRepository {
    PlayerGame findOrCreate(String username, long gameId);
    void save(PlayerGame playerGame);
    List<PlayerGame> findByGame(long gameId);
    List<PlayerGame> findPlayerGameByUsername(String username);
    Optional<PlayerGame> findByUsernameAndGame(String username, long gameId);
}