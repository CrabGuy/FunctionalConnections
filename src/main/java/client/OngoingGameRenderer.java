package client;

import shared.DataContracts;

public final class OngoingGameRenderer implements GameBoardRenderer {
    @Override
    public void render(DataContracts.GameStateDto board) {
        if (!(board instanceof DataContracts.OngoingGameStateDto ongoing)) return;

        System.out.println("\n           CONNECTIONS BOARD");
        System.out.println("==========================================");
        System.out.printf("Score: %d%n", ongoing.score());
        long remainingMs = ongoing.remainingTimeMs() != null ? ongoing.remainingTimeMs() : 0;
        long seconds = remainingMs / 1000;
        System.out.printf("Time left: %02d:%02d%n", seconds / 60, seconds % 60);
        System.out.println("------------------------------------------");

        if (ongoing.solvedGroups() != null && !ongoing.solvedGroups().isEmpty()) {
            System.out.println("Solved Groups:");
            for (int i = 0; i < ongoing.solvedGroups().size(); i++) {
                DataContracts.SolvedGroupDto group = ongoing.solvedGroups().get(i);
                System.out.println("  Group " + (i + 1) + " (" + group.category() + "): " + String.join(", ", group.words()));
            }
            System.out.println("------------------------------------------");
        }

        if (ongoing.remainingWords() != null && !ongoing.remainingWords().isEmpty()) {
            System.out.println("Remaining Words:");
            for (int i = 0; i < ongoing.remainingWords().size(); i++) {
                System.out.printf("%2d) %-15s%s", (i + 1), ongoing.remainingWords().get(i), (i + 1) % 4 == 0 ? "\n" : "");
            }
            if (ongoing.remainingWords().size() % 4 != 0) System.out.println();
            System.out.println("------------------------------------------");
        }
        System.out.print("Mistakes made: " + ongoing.mistakes());
    }
}