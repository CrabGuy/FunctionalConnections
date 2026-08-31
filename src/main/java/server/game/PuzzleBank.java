package server.game;

import com.fasterxml.jackson.databind.type.TypeFactory;
import shared.JsonCodec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public final class PuzzleBank {
    private final List<List<GameSession.WordGroup>> puzzles;

    public PuzzleBank(String filePath) {
        this.puzzles = loadPuzzleBank(filePath);
    }

    public List<GameSession.WordGroup> getPuzzleForGameId(long gameId) {
        int index = (int) Math.floorMod(gameId, puzzles.size());
        return puzzles.get(index);
    }

    private static List<List<GameSession.WordGroup>> loadPuzzleBank(String filePath) {
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            throw new RuntimeException("Puzzle bank file not found at " + path.toAbsolutePath()
                    + ". Place a valid puzzle data file there before starting the server.");
        }
        try {
            String jsonContent = Files.readString(path);
            var typeFactory = TypeFactory.defaultInstance();
            var type = typeFactory.constructCollectionType(List.class, GameDataDto.class);
            List<GameDataDto> rawGames = JsonCodec.deserialize(jsonContent, type);
            if (rawGames == null || rawGames.isEmpty()) {
                throw new RuntimeException("Puzzle bank file at " + path.toAbsolutePath() + " contains no games.");
            }
            return rawGames.stream()
                    .map(game -> game.groups().stream()
                            .map(group -> new GameSession.WordGroup(group.theme(), Set.copyOf(group.words())))
                            .toList())
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load puzzle bank from " + path.toAbsolutePath(), e);
        }
    }

    private record GameDataDto(int gameId, List<WordGroupDto> groups) {}
    private record WordGroupDto(String theme, List<String> words) {}
}