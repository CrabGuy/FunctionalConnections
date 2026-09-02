package shared.dto;
public record LogoutRequest(String accountToken) implements ApiRequest {
    @Override
    public String getOperation() {
        return "logout";
    }
}
