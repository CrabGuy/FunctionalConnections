package shared;

public enum ErrorCode {
    INVALID_REQUEST("INVALID_REQUEST", "Invalid request format."),
    INVALID_CREDENTIALS_FORMAT("INVALID_CREDENTIALS_FORMAT", "Username or password has an invalid format."),
    USERNAME_ALREADY_EXISTS("USERNAME_ALREADY_EXISTS", "Username is already taken."),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "The supplied credentials are invalid."),
    INVALID_USERNAME_OR_PASSWORD("INVALID_USERNAME_OR_PASSWORD", "Invalid username or password."),
    UNAUTHORIZED_OR_USER_MISMATCH("UNAUTHORIZED_OR_USER_MISMATCH", "You are not authorized for this action."),
    INVALID_USERNAME("INVALID_USERNAME", "Username is invalid."),
    TARGET_USERNAME_TAKEN("TARGET_USERNAME_TAKEN", "The requested username is already taken."),
    USER_NOT_LOGGED_IN("USER_NOT_LOGGED_IN", "You must be logged in."),
    USER_NOT_FOUND("USER_NOT_FOUND", "User not found."),
    MALFORMED_PROPOSAL("MALFORMED_PROPOSAL", "The proposal format is invalid."),
    PROPOSAL_WRONG_SIZE("PROPOSAL_WRONG_SIZE", "Proposal must contain exactly 4 words."),
    PROPOSAL_DUPLICATE_WORDS("PROPOSAL_DUPLICATE_WORDS", "Proposal contains duplicate words."),
    PROPOSAL_NOT_IN_PUZZLE("PROPOSAL_NOT_IN_PUZZLE", "One or more words are not in the puzzle."),
    WORD_ALREADY_SOLVED("WORD_ALREADY_SOLVED", "One or more words are already solved."),
    DUPLICATE_PROPOSAL("DUPLICATE_PROPOSAL", "This proposal was already submitted."),
    GAME_ALREADY_OVER("GAME_ALREADY_OVER", "The game is already over."),
    GAME_NOT_FOUND("GAME_NOT_FOUND", "Game not found."),
    PLAYER_NOT_FOUND("PLAYER_NOT_FOUND", "Player not found."),
    UNKNOWN_REQUEST("UNKNOWN_REQUEST", "Unknown request.");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() { return code; }
    public String getMessage() { return message; }
}