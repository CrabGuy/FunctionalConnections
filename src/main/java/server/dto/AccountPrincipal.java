package server.dto;

/**
 * The identity recovered from a verified account token. Not persisted —
 * this is the result of {@link server.account.TokenSigner#verify(String)}
 * or {@link server.account.AccountService#resolve(String)}, valid only
 * for the lifetime of the request that produced it.
 *
 * @param username the authenticated user
 * @param expiresAt epoch-millis after which the originating token is no longer valid
 */
public record AccountPrincipal(String username, long expiresAt) {
}