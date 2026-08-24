package client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record ClientState(
        String currentUser,
        Long currentGameId,
        List<Set<String>> solvedGroups,
        String status,
        int mistakesMade,
        int maxMistakes
) {
    public static ClientState empty() {
        return new ClientState(null, null, List.of(), "IN_PROGRESS", 0, 5);
    }

    public ClientState withCurrentUser(String currentUser) {
        return new ClientState(currentUser, currentGameId, solvedGroups, status, mistakesMade, maxMistakes);
    }

    public ClientState withGame(Long gameId) {
        if (currentGameId != null && currentGameId.equals(gameId)) {
            return this;
        }
        return new ClientState(currentUser, gameId, List.of(), "IN_PROGRESS", 0, maxMistakes);
    }

    public ClientState withSolvedGroup(Set<String> words) {
        List<Set<String>> updated = new ArrayList<>(solvedGroups);
        updated.add(Set.copyOf(words));
        return new ClientState(currentUser, currentGameId, List.copyOf(updated), status, mistakesMade, maxMistakes);
    }

    public ClientState withStatus(String status) {
        return new ClientState(currentUser, currentGameId, solvedGroups, status, mistakesMade, maxMistakes);
    }

    public ClientState withMistakesMade(int mistakesMade) {
        return new ClientState(currentUser, currentGameId, solvedGroups, status, mistakesMade, maxMistakes);
    }

    public ClientState withReset() {
        return new ClientState(null, null, List.of(), "IN_PROGRESS", 0, maxMistakes);
    }

    public Set<String> getAllSolvedWords() {
        Set<String> all = new HashSet<>();
        solvedGroups.forEach(all::addAll);
        return Collections.unmodifiableSet(all);
    }

    public boolean isGameOver() {
        return "WON".equalsIgnoreCase(status) || "LOST".equalsIgnoreCase(status);
    }
}