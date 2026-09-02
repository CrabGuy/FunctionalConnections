package shared.dto;

/**
 * Enumeration of all possible error codes that the server may return
 * in an {@link ApiResponse}.
 */
public enum ErrorCode {
    USERNAME_ALREADY_REGISTERED,
    INCORRECT_PASSWORD,
    USER_NOT_LOGGED_IN,
    MALFORMED_PROPOSAL,
    GAME_NOT_FOUND,
    INVALID_GAME_ID,
    NEW_USERNAME_ALREADY_TAKEN,
    INTERNAL_ERROR,
    WORDS_ALREADY_GROUPED,
    UNKNOWN_WORDS_IN_PROPOSAL
}
