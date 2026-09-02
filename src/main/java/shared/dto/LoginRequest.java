package shared.dto;
public record LoginRequest(String username, String password, int udpPort) implements ApiRequest {
    @Override
    public String getOperation() {
        return "login";
    }
}
