package shared.dto;
public record RequestPlayerStatsRequest(String accountToken) implements ApiRequest {
    @Override
    public String getOperation() {
        return "requestPlayerStats";
    }
}
