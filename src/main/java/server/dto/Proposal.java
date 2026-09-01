package server.dto;

import java.util.Set;

/**
 * A single proposal made by a player during a game.
 * The proposal is simply the set of four words submitted.
 * Correctness is not stored; it is derived by comparing the set
 * to the solution groups of the corresponding game.
 *
 * @param words the four words proposed as an immutable set.
 */
public record Proposal(Set<String> words) {
    public Proposal {
        words = Set.copyOf(words);
    }
}
