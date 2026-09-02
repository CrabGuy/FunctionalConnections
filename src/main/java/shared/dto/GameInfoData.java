package shared.dto;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
