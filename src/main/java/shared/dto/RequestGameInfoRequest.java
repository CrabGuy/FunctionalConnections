package shared.dto;
public record RequestGameInfoRequest(String accountToken, Long gameId) implements ApiRequest {
    @Override
    public String getOperation() {
        return "requestGameInfo";
    }
}
