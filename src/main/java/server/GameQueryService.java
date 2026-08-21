package server;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class GameQueryService {
    private final GameManager gameManager;
    private final UserManager userManager;

    public GameQueryService(GameManager gameManager, UserManager userManager) {
        this.gameManager = gameManager;
        this.userManager = userManager;
    }

    public Optional<GameManager.Game> resolveGame(Long requestedId) {
        long currentId = gameManager.getCurrentGameId();
        if (requestedId == null || requestedId.longValue() == currentId) {
            return Optional.of(gameManager.getActiveGame());
        }
        return gameManager.getGame(requestedId);
    }

    public GameManager.PlayerProgress progress(GameManager.Game game, String player) {
        return game.playerStates().getOrDefault(player, new GameManager.PlayerProgress(List.of()));
    }

    public Set<String> allWords(GameManager.Game game) {
        return game.wordGroups().stream()
                .flatMap(group -> group.words().stream())
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }

    public Set<String> solvedWords(GameManager.Game game, String player) {
        return progress(game, player).history().stream()
                .filter(GameManager.Guess::isCorrect)
                .flatMap(guess -> guess.words().stream())
                .collect(Collectors.toSet());
    }

    public void recordCompletedGameIfEnded(GameManager.Game game, String player) {
        GameManager.Status status = gameManager.getPlayerStatus(game, player);
        if (status != GameManager.Status.IN_PROGRESS && game.playerStates().containsKey(player)) {
            GameManager.PlayerProgress playerProgress = progress(game, player);
            userManager.recordCompletedGame(
                    player,
                    game.id(),
                    (int) playerProgress.mistakesMade(),
                    (int) playerProgress.solvedCount()
            );
        }
    }
}