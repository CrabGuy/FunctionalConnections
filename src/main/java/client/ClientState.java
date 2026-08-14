package client;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class ClientState {
    private String currentUser;
    private Long currentGameId;
    private final Set<String> solvedWords = new HashSet<>();

    public String getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(String currentUser) {
        this.currentUser = currentUser;
    }

    public Long getCurrentGameId() {
        return currentGameId;
    }

    public void updateGame(Long gameId) {
        if (this.currentGameId == null || !this.currentGameId.equals(gameId)) {
            this.currentGameId = gameId;
            this.solvedWords.clear();
        }
    }

    public void addSolvedWords(Set<String> words) {
        this.solvedWords.addAll(words);
    }

    public Set<String> getSolvedWords() {
        return Collections.unmodifiableSet(solvedWords);
    }

    public void reset() {
        this.currentUser = null;
        this.currentGameId = null;
        this.solvedWords.clear();
    }
}