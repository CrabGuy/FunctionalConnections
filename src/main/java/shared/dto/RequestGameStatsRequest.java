package shared.dto;

/**
 * Request for aggregated game statistics.
 * Operation: "requestGameStats"
 *
 * @param gameId the ID of the game, or null to indicate the current game.
 */
public record RequestGameStatsRequest(Long gameId) implements ApiRequest {
    @Override
    public String getOperation() {
        return "requestGameStats";
    }
}
