package shared.dto;
public record RegisterRequest(String username, String password) implements ApiRequest {
    @Override
    public String getOperation() {
        return "register";
    }
}
