package shared.dto;

import java.util.Map;

/**
 * Distribution of wins by mistake count and aggregated loss/not finished counts.
 *
 * @param wonByMistakes map from number of mistakes (0-3) to number of wins
 * @param lost total number of losses
 * @param notFinished total number of incomplete games
 */
public record MistakeHistogram(Map<Integer, Integer> wonByMistakes, int lost, int notFinished) {

  public MistakeHistogram {
    wonByMistakes = Map.copyOf(wonByMistakes);
  }
}
