package shared.dto;

public record RegisterRequest(String operation, String username, String psw) implements ApiRequest {
    public RegisterRequest(String username, String psw) {
        this("register", username, psw);
    }
}
