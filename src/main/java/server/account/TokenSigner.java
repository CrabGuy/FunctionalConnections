package server.account;

import server.account.exceptions.InvalidTokenException;
import server.dto.AccountPrincipal;

/**
 * Issues and verifies signed, self-describing (JWT-style) account tokens.
 * No server-side session store is used — a valid token is sufficient proof
 * of identity on its own, verifiable via signature alone.
 */
public interface TokenSigner {

    /**
     * @param username subject to encode into the token
     * @param expiresAt epoch-millis after which the token must be rejected
     * @return a signed, encoded token string
     */
    String sign(String username, long expiresAt);

    /**
     * @param token a token previously produced by {@link #sign}
     * @return the identity and expiry encoded in the token
     * @throws InvalidTokenException if the signature is invalid, the token
     *         is malformed, or {@code expiresAt} is in the past
     */
    AccountPrincipal verify(String token) throws InvalidTokenException;
}