package shared.dto;

import java.util.Map;

public record MistakeHistogram(Map<Integer, Integer> wonByMistakes, int lost, int notFinished) {
  public MistakeHistogram {
    wonByMistakes = Map.copyOf(wonByMistakes);
  }
}