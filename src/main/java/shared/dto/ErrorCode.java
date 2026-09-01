package shared.dto;

/**
 * Enumeration of all possible error codes that the server may return
 * in an {@link ApiResponse}.
 */
public enum ErrorCode {
    /** Username already exists during registration. */
    USERNAME_ALREADY_REGISTERED,
    /** Incorrect password provided during login or credential update. */
    INCORRECT_PASSWORD,
    /** User is not logged in (no valid token) for an operation requiring authentication. */
    USER_NOT_LOGGED_IN,
    /** Proposal is malformed: wrong number of words, already grouped, or invalid words. */
    MALFORMED_PROPOSAL,
    /** Requested game ID does not exist or is not available. */
    GAME_NOT_FOUND,
    /** Invalid game ID (e.g., negative or zero when current game expected). */
    INVALID_GAME_ID,
    /** New username already taken during credential update. */
    NEW_USERNAME_ALREADY_TAKEN,
    /** Generic internal server error. */
    INTERNAL_ERROR
}
