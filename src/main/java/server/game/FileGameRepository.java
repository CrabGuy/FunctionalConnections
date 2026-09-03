package server.game;

import com.google.gson.stream.JsonReader;
import server.dto.GameWordGroups;
import server.dto.WordGroup;
import server.game.exceptions.GameNotFoundException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of GameRepository that reads game data lazily from a JSON file.
 * The file is expected to be a JSON array of objects with fields:
 *   - gameId (long)
 *   - groups (array of objects with "theme" and "words")
 * The repository maps actual game slot IDs (time‑based) to file entries via modulo,
 * assuming the file's entries are contiguous and start at index 0.
 */
public record FileGameRepository(String gameDataFile) implements GameRepository {

    // Static cache for total number of games per file path (computed lazily, once).
    private static final Map<String, Integer> TOTAL_GAMES_CACHE = new ConcurrentHashMap<>();

    @Override
    public GameWordGroups loadById(long gameId) throws GameNotFoundException {
        int total = getTotalGames();
        if (total == 0) {
            throw new GameNotFoundException(gameId);
        }
        int index = (int) (gameId % total);
        try (JsonReader reader = new JsonReader(new FileReader(gameDataFile))) {
            reader.beginArray();
            int current = 0;
            while (reader.hasNext()) {
                if (current == index) {
                    return parseGameObject(reader, gameId);
                }
                reader.skipValue();
                current++;
            }
            reader.endArray();
            throw new GameNotFoundException(gameId);
        } catch (IOException e) {
            throw new RuntimeException("Error reading game data file: " + gameDataFile, e);
        }
    }

    @Override
    public boolean exists(long gameId) {
        int total = getTotalGames();
        if (total == 0) return false;
        int index = (int) (gameId % total);
        return index >= 0 && index < total;
    }

    private int getTotalGames() {
        return TOTAL_GAMES_CACHE.computeIfAbsent(gameDataFile, path -> {
            try (JsonReader reader = new JsonReader(new FileReader(path))) {
                reader.beginArray();
                int count = 0;
                while (reader.hasNext()) {
                    reader.skipValue();
                    count++;
                }
                reader.endArray();
                return count;
            } catch (IOException e) {
                throw new RuntimeException("Error reading game data file: " + path, e);
            }
        });
    }

    private GameWordGroups parseGameObject(JsonReader reader, long actualGameId) throws IOException {
        reader.beginObject();
        List<WordGroup> groups = new ArrayList<>();
        while (reader.hasNext()) {
            String fieldName = reader.nextName();
            if ("groups".equals(fieldName)) {
                reader.beginArray();
                while (reader.hasNext()) {
                    groups.add(parseGroup(reader));
                }
                reader.endArray();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return new GameWordGroups(actualGameId, groups);
    }

    private WordGroup parseGroup(JsonReader reader) throws IOException {
        reader.beginObject();
        String theme = null;
        List<String> words = new ArrayList<>();
        while (reader.hasNext()) {
            String fieldName = reader.nextName();
            if ("theme".equals(fieldName)) {
                theme = reader.nextString();
            } else if ("words".equals(fieldName)) {
                reader.beginArray();
                while (reader.hasNext()) {
                    words.add(reader.nextString());
                }
                reader.endArray();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        if (theme == null) {
            throw new IOException("Group missing theme");
        }
        return new WordGroup(theme, words);
    }
}