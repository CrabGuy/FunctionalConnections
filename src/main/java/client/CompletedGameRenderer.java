package client;

import shared.DataContracts;

public final class CompletedGameRenderer implements GameBoardRenderer {
    @Override
    public void render(DataContracts.GameStateDto board) {
        if (!(board instanceof DataContracts.CompletedGameStateDto completed)) return;

        System.out.println("\n           CONNECTIONS BOARD");
        System.out.println("==========================================");
        System.out.printf("Score: %d%n", completed.score());
        System.out.println("------------------------------------------");

        if (completed.solvedGroups() != null && !completed.solvedGroups().isEmpty()) {
            System.out.println("Solved Groups:");
            for (int i = 0; i < completed.solvedGroups().size(); i++) {
                DataContracts.SolvedGroupDto group = completed.solvedGroups().get(i);
                System.out.println("  Group " + (i + 1) + " (" + group.category() + "): " + String.join(", ", group.words()));
            }
            System.out.println("------------------------------------------");
        }

        if (completed.allGroups() != null && !completed.allGroups().isEmpty()) {
            System.out.println("Correct Groups:");
            for (DataContracts.GameGroupDto group : completed.allGroups()) {
                System.out.println("  " + group.category() + ": " + String.join(", ", group.words()));
            }
        }
        System.out.print("Mistakes made: " + completed.mistakes());
    }
}