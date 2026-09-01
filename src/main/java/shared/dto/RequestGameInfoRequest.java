package shared.dto;

/**
 * Request for game information (ongoing or completed).
 * Operation: "requestGameInfo"
 *
 * @param accountToken the JWT token identifying the logged-in user.
 * @param gameId      the ID of the game, or null to indicate the current game.
 */
public record RequestGameInfoRequest(String accountToken, Long gameId) implements ApiRequest {
    @Override
    public String getOperation() {
        return "requestGameInfo";
    }
}
