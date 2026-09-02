package shared.dto;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Data payload for the "requestGameInfo" operation.
 * Contains only the raw information necessary for the client to reconstruct
 * its game state and compute derived values (score, mistakes, remaining words,
 * player status) using pure functions.
 *
 * @param gameId              the ID of the game.
 * @param expiresAt           Unix epoch timestamp (seconds) when the game ends; valid even if completed.
 * @param words               the full list of 16 words for this game.
 * @param correctGuesses      list of word sets that the player correctly guessed.
 * @param wrongGuesses        list of word sets that the player incorrectly guessed.
 * @param correctGroups       the full solution (all 4 groups); only present if completed.
 */
public record GameInfoData(
        long gameId,
        long expiresAt,
        List<String> words,
        List<Set<String>> correctGuesses,
        List<Set<String>> wrongGuesses,
        List<List<String>> correctGroups
) {
    public GameInfoData {
        words = List.copyOf(words);
        correctGuesses = correctGuesses.stream()
                .map(Set::copyOf)
                .collect(Collectors.toUnmodifiableList());
        wrongGuesses = wrongGuesses.stream()
                .map(Set::copyOf)
                .collect(Collectors.toUnmodifiableList());
        if (correctGroups != null) {
            correctGroups = correctGroups.stream()
                    .map(List::copyOf)
                    .collect(Collectors.toUnmodifiableList());
        }
    }
}
