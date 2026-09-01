package client;

import java.util.Arrays;
import java.util.List;

public final class HistogramFormatter {
    public static List<String> format(String histogram) {
        if (histogram == null || histogram.isBlank() || "NONE".equals(histogram)) {
            return List.of("  No completed games yet.");
        }
        return Arrays.stream(histogram.split(","))
                .map(entry -> {
                    String[] parts = entry.split(":");
                    if (parts.length >= 2) {
                        return String.format("  %s mistakes: %s game(s)", parts[0], parts[1]);
                    }
                    return entry;
                })
                .toList();
    }
}