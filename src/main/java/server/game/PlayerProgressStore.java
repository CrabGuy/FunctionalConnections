package server.game;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public final class PlayerProgressStore {
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, PlayerProgress>> store = new ConcurrentHashMap<>();

    public Optional<PlayerProgress> get(long gameId, String username) {
        var inner = store.get(gameId);
        return inner == null ? Optional.empty() : Optional.ofNullable(inner.get(username));
    }

    public Set<String> participantsFor(long gameId) {
        var inner = store.get(gameId);
        return inner == null ? Set.of() : Set.copyOf(inner.keySet());
    }

    public void put(long gameId, String username, PlayerProgress progress) {
        store.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
             .put(username, progress);
    }

    public PlayerProgress compute(long gameId, String username,
                                  BiFunction<String, PlayerProgress, PlayerProgress> remappingFunction) {
        return store.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
                    .compute(username, remappingFunction);
    }

    public Map<Long, Map<String, PlayerProgress>> snapshot() {
        Map<Long, Map<String, PlayerProgress>> copy = new ConcurrentHashMap<>();
        store.forEach((gameId, innerMap) -> {
            copy.put(gameId, Map.copyOf(innerMap));
        });
        return copy;
    }

    public void loadSnapshot(Map<Long, Map<String, PlayerProgress>> snapshot) {
        store.clear();
        snapshot.forEach((gameId, innerMap) -> {
            ConcurrentHashMap<String, PlayerProgress> newInner = new ConcurrentHashMap<>();
            newInner.putAll(innerMap);
            store.put(gameId, newInner);
        });
    }

    public void removeGame(long gameId) {
        store.remove(gameId);
    }
}