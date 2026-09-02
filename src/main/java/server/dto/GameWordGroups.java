package server.dto;
import java.util.List;
public record GameWordGroups(long gameId, List<WordGroup> groups) {
    public GameWordGroups {
        groups = List.copyOf(groups);
    }
}
