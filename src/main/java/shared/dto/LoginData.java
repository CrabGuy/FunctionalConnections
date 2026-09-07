package shared.dto;

/**
 * Response data for a successful login.
 *
 * @param accountToken the signed account token
 */
public record LoginData(String accountToken) {}
