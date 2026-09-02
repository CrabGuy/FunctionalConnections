package server.dto;
import java.util.List;
public record WordGroup(String theme, List<String> words) {
    public WordGroup {
        words = List.copyOf(words);
    }
}
