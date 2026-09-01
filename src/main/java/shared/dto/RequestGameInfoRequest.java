package shared.dto;

/**
 * Request for game information (ongoing or completed).
 * Operation: "requestGameInfo"
 *
 * @param gameId the ID of the game, or null to indicate the current game.
 */
public record RequestGameInfoRequest(Long gameId) implements ApiRequest {
    @Override
    public String getOperation() {
        return "requestGameInfo";
    }
}
