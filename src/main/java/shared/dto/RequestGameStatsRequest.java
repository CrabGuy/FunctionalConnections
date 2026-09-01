package shared.dto;

/**
 * Request for aggregated game statistics.
 * Operation: "requestGameStats"
 *
 * @param accountToken the JWT token identifying the logged-in user.
 * @param gameId      the ID of the game, or null to indicate the current game.
 */
public record RequestGameStatsRequest(String accountToken, Long gameId) implements ApiRequest {
    @Override
    public String getOperation() {
        return "requestGameStats";
    }
}
