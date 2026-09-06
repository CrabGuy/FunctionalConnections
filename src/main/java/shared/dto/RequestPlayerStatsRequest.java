package shared.dto;

public record RequestPlayerStatsRequest(String operation, String accountToken) implements ApiRequest {
    public RequestPlayerStatsRequest(String accountToken) {
        this("requestPlayerStats", accountToken);
    }
}
