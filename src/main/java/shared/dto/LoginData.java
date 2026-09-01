package shared.dto;

/**
 * Payload for a successful login response.
 * Contains the JWT account token.
 */
public record LoginData(String accountToken) {
}
