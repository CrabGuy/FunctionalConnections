package shared.dto;

/**
 * Request for the authenticated user's personal statistics.
 * Operation: "requestPlayerStats"
 *
 * @param accountToken the JWT token identifying the logged-in user.
 */
public record RequestPlayerStatsRequest(String accountToken) implements ApiRequest {
    @Override
    public String getOperation() {
        return "requestPlayerStats";
    }
}
