package server.handlers;

import server.GameManager;
import server.ServiceContext;
import shared.ErrorCode;
import shared.Request;
import shared.Response;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class SendAnswerHandler implements RequestHandler<Request.SendAnswer> {
    @Override
    public Response handle(Request.SendAnswer answer, ServiceContext ctx, String currentUser) {
        if (currentUser == null) {
            return Response.error(ErrorCode.USER_NOT_LOGGED_IN);
        }
        if (answer.words() == null) {
            return Response.error(ErrorCode.MALFORMED_PROPOSAL);
        }
        List<String> normalized = answer.words().stream()
                .map(String::trim)
                .filter(word -> !word.isBlank())
                .map(String::toUpperCase)
                .toList();
        if (normalized.size() != 4 || normalized.stream().distinct().count() != 4) {
            return Response.error(ErrorCode.MALFORMED_PROPOSAL);
        }
        GameManager.Game game = ctx.gameManager().getActiveGame();
        GameManager.Status currentStatus = ctx.gameManager().getPlayerStatus(game, currentUser);
        if (currentStatus != GameManager.Status.IN_PROGRESS) {
            return Response.error(ErrorCode.MALFORMED_PROPOSAL_OR_GAME_OVER);
        }
        Set<String> allValidWords = ctx.gameQuery().allWords(game);
        if (!allValidWords.containsAll(normalized)) {
            return Response.error(ErrorCode.INVALID_WORDS_NOT_IN_PUZZLE);
        }
        Set<String> solvedWords = ctx.gameQuery().solvedWords(game, currentUser);
        if (normalized.stream().anyMatch(solvedWords::contains)) {
            return Response.error(ErrorCode.WORDS_ALREADY_SOLVED);
        }
        Set<String> guess = new HashSet<>(normalized);
        GameManager.PlayerProgress currentProgress = ctx.gameQuery().progress(game, currentUser);
        if (currentProgress.containsGuess(guess)) {
            return Response.error(ErrorCode.DUPLICATE_PROPOSAL);
        }
        Optional<GameManager.Game> updated = ctx.gameManager().processGuess(game.id(), currentUser, guess);
        if (updated.isEmpty()) {
            return Response.error(ErrorCode.MALFORMED_PROPOSAL_OR_GAME_OVER);
        }
        GameManager.Game newGame = updated.get();
        GameManager.PlayerProgress newProgress = ctx.gameQuery().progress(newGame, currentUser);
        boolean lastCorrect = newProgress.history().get(newProgress.history().size() - 1).isCorrect();
        ctx.gameQuery().recordCompletedGameIfEnded(newGame, currentUser);
        GameManager.Status status = ctx.gameManager().getPlayerStatus(newGame, currentUser);
        return Response.success("STATUS:" + status + " | LAST GUESS CORRECT: " + lastCorrect);
    }
}