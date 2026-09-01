package shared.dto;

/**
 * Request for the authenticated user's personal statistics.
 * Operation: "requestPlayerStats"
 */
public record RequestPlayerStatsRequest() implements ApiRequest {
    @Override
    public String getOperation() {
        return "requestPlayerStats";
    }
}
