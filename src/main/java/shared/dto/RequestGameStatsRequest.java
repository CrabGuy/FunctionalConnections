package shared.dto;
public record RequestGameStatsRequest(String accountToken, Long gameId) implements ApiRequest {
    @Override
    public String getOperation() {
        return "requestGameStats";
    }
}
