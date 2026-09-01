package server.storage;

import server.User;
import server.game.PlayerProgress;
import java.util.Map;

public record ServerSnapshot(
        Map<String, User> users,
        Map<Long, Map<String, PlayerProgress>> playerProgress
) {}