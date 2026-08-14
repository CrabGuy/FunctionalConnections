package client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ClientState {
    private String currentUser;
    private Long currentGameId;
    private final List<Set<String>> solvedGroups = new ArrayList<>();
    private String status = "IN_PROGRESS";
    private int mistakesMade = 0;
    private int maxMistakes = 5;

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
            this.solvedGroups.clear();
            this.status = "IN_PROGRESS";
            this.mistakesMade = 0;
        }
    }

    public void addSolvedGroup(Set<String> words) {
        this.solvedGroups.add(Set.copyOf(words));
    }

    public List<Set<String>> getSolvedGroups() {
        return Collections.unmodifiableList(solvedGroups);
    }

    public Set<String> getAllSolvedWords() {
        Set<String> all = new HashSet<>();
        solvedGroups.forEach(all::addAll);
        return Collections.unmodifiableSet(all);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getMistakesMade() {
        return mistakesMade;
    }

    public void setMistakesMade(int mistakesMade) {
        this.mistakesMade = mistakesMade;
    }

    public int getMaxMistakes() {
        return maxMistakes;
    }

    public boolean isGameOver() {
        return "WON".equalsIgnoreCase(status) || "LOST".equalsIgnoreCase(status);
    }

    public void reset() {
        this.currentUser = null;
        this.currentGameId = null;
        this.solvedGroups.clear();
        this.status = "IN_PROGRESS";
        this.mistakesMade = 0;
    }
}