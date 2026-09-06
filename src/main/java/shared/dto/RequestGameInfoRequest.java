package shared.dto;

public record RequestGameInfoRequest(String operation, String accountToken, Long gameId) implements ApiRequest {
    public RequestGameInfoRequest(String accountToken, Long gameId) {
        this("requestGameInfo", accountToken, gameId);
    }
}
