package server.game;

import com.google.gson.stream.JsonReader;
import server.dto.GameWordGroups;
import server.dto.WordGroup;
import server.game.exceptions.GameNotFoundException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of {@link GameRepository} that loads game word groups from a JSON file.
 * The file is expected to be an array of objects, each containing a "groups" array.
 * Each group has a "theme" and a "words" array.
 * <p>
 * The total number of games is lazily computed once per repository instance and cached.
 * This avoids reading the file repeatedly for size checks.
 */
public final class FileGameRepository implements GameRepository {
    private final String gameDataFile;
    private final AtomicInteger totalGames = new AtomicInteger(-1); // -1 means not loaded yet

    public FileGameRepository(String gameDataFile) {
        this.gameDataFile = gameDataFile;
    }

    @Override
    public GameWordGroups loadById(long gameId) throws GameNotFoundException {
        if (gameId < 0) {
            throw new GameNotFoundException(gameId);
        }
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
        if (gameId < 0) {
            return false;
        }
        int total = getTotalGames();
        if (total == 0) return false;
        int index = (int) (gameId % total);
        return index >= 0 && index < total;
    }

    /**
     * Lazily loads and caches the total number of games in the file.
     * Uses an AtomicInteger for visibility and a synchronized block to prevent
     * multiple threads from reading the file concurrently during initial load.
     */
    private int getTotalGames() {
        int cached = totalGames.get();
        if (cached >= 0) {
            return cached;
        }
        synchronized (this) {
            cached = totalGames.get();
            if (cached >= 0) {
                return cached;
            }
            int count = countGamesInFile();
            totalGames.set(count);
            return count;
        }
    }

    /**
     * Counts the number of elements in the JSON array at the top level.
     */
    private int countGamesInFile() {
        try (JsonReader reader = new JsonReader(new FileReader(gameDataFile))) {
            reader.beginArray();
            int count = 0;
            while (reader.hasNext()) {
                reader.skipValue();
                count++;
            }
            reader.endArray();
            return count;
        } catch (IOException e) {
            throw new RuntimeException("Error reading game data file: " + gameDataFile, e);
        }
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