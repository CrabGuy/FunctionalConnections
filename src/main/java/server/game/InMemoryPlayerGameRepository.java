package server.game;

import server.dto.PlayerGame;
import server.dto.PlayerGameKey;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class InMemoryPlayerGameRepository implements PlayerGameRepository {

    private final ConcurrentHashMap<PlayerGameKey, PlayerGame> store = new ConcurrentHashMap<>();

    @Override
    public PlayerGame findOrCreate(String username, long gameId) {
        PlayerGameKey key = new PlayerGameKey(username, gameId);
        return store.computeIfAbsent(key, k -> new PlayerGame(username, gameId, List.of()));
    }

    @Override
    public void save(PlayerGame playerGame) {
        PlayerGameKey key = new PlayerGameKey(playerGame.username(), playerGame.gameId());
        store.put(key, playerGame);
    }

    @Override
    public List<PlayerGame> findByGame(long gameId) {
        return store.values().stream()
                .filter(pg -> pg.gameId() == gameId)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<PlayerGame> findPlayerGameByUsername(String username) {
        return store.values().stream()
                .filter(pg -> pg.username().equals(username))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Optional<PlayerGame> findByUsernameAndGame(String username, long gameId) {
        PlayerGameKey key = new PlayerGameKey(username, gameId);
        return Optional.ofNullable(store.get(key));
    }

    @Override
    public Set<String> findAllUsernames() {
        return store.values().stream()
                .map(PlayerGame::username)
                .collect(Collectors.toUnmodifiableSet());
    }
}