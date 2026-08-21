package client;

import java.util.Map;

public final class ErrorDisplay {
    private static final Map<String, String> MESSAGES = Map.ofEntries(
        Map.entry("INVALID_REQUEST", "Invalid request format."),
        Map.entry("INVALID_CREDENTIALS_FORMAT", "Username or password has an invalid format."),
        Map.entry("USERNAME_ALREADY_EXISTS", "Username is already taken."),
        Map.entry("INVALID_CREDENTIALS", "The supplied credentials are invalid."),
        Map.entry("INVALID_USERNAME_OR_PASSWORD", "Invalid username or password."),
        Map.entry("UNAUTHORIZED_OR_USER_MISMATCH", "You are not authorized for this action."),
        Map.entry("INVALID_USERNAME", "Username is invalid."),
        Map.entry("TARGET_USERNAME_TAKEN", "The requested username is already taken."),
        Map.entry("USER_NOT_LOGGED_IN", "You must be logged in."),
        Map.entry("USER_NOT_FOUND", "User not found."),
        Map.entry("MALFORMED_PROPOSAL", "The proposal format is invalid."),
        Map.entry("MALFORMED_PROPOSAL_OR_GAME_OVER", "Invalid proposal or game is already over."),
        Map.entry("INVALID_WORDS_NOT_IN_PUZZLE", "One or more words are not in the puzzle."),
        Map.entry("WORDS_ALREADY_SOLVED", "One or more words are already solved."),
        Map.entry("DUPLICATE_PROPOSAL", "This proposal was already submitted."),
        Map.entry("GAME_NOT_FOUND", "Game not found."),
        Map.entry("PLAYER_NOT_FOUND", "Player not found."),
        Map.entry("UNKNOWN_REQUEST", "Unknown request.")
    );

    private ErrorDisplay() {}

    public static String message(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            return "Unknown error.";
        }
        return MESSAGES.getOrDefault(errorCode, errorCode);
    }
}