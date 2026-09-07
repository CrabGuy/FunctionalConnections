package server.game;

import com.google.gson.stream.JsonReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import server.dto.GameWordGroups;
import server.dto.WordGroup;
import server.game.exceptions.GameNotFoundException;

/**
 * Implementation of {@link GameRepository} that reads games from a JSON file. The file contains an
 * array of game objects, and the repository supports loading games by ID using modulo indexing.
 */
public final class FileGameRepository implements GameRepository {

  private final String gameDataFile;
  private final AtomicInteger totalGames = new AtomicInteger(-1);

  /**
   * Constructs a new repository with the given file path.
   *
   * @param gameDataFile the path to the JSON file containing game definitions
   */
  public FileGameRepository(String gameDataFile) {
    this.gameDataFile = gameDataFile;
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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
   * Returns the total number of games in the file, caching the result.
   *
   * @return the total number of games
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
   * Counts the number of game objects in the file.
   *
   * @return the count
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

  /**
   * Parses a single game object from the JSON reader.
   *
   * @param reader the JSON reader positioned at the start of the object
   * @param actualGameId the actual game ID to assign
   * @return the parsed {@link GameWordGroups}
   * @throws IOException if a parsing error occurs
   */
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

  /**
   * Parses a single word group object.
   *
   * @param reader the JSON reader positioned at the start of the group object
   * @return the parsed {@link WordGroup}
   * @throws IOException if a parsing error occurs
   */
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
