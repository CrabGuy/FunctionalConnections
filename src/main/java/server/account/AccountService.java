package server.account;

import server.dto.AccountPrincipal;
import shared.dto.LoginData;
import shared.dto.RegisterData;
import shared.dto.UpdateCredentialsData;

/**
 * Account lifecycle: registration, authentication, credential updates,
 * and token resolution. Every other server-side service depends on
 * {@link #resolve} to authenticate the caller of a request.
 */
public interface AccountService {

    /**
     * Registers a new account with a hashed password.
     *
     * @param username desired username; must not already be registered
     * @param password plaintext password, hashed before storage
     * @return confirmation of the created username
     * @throws UsernameAlreadyRegisteredException if {@code username} is already taken
     */
    RegisterData register(String username, String password);

    /**
     * Authenticates a user and issues a signed account token, registering
     * the client's UDP endpoint for asynchronous notifications.
     *
     * @param username account to authenticate
     * @param password plaintext password to verify against the stored hash
     * @param udpPort  UDP port the client is listening on for notifications
     * @param remoteAddress source address of the client's TCP connection;
     *                      combined with {@code udpPort} to register the
     *                      client in {@link NotificationRegistry}
     * @return a signed account token
     * @throws IncorrectPasswordException if {@code username} does not exist
     *         or {@code password} does not match the stored hash
     */
    LoginData login(String username, String password, int udpPort, String remoteAddress);

    /**
     * Logs a user out, removing their UDP notification registration.
     *
     * <p>Because tokens are self-describing and signed rather than tracked
     * server-side, logout cannot revoke the token itself — it remains
     * cryptographically valid until it expires naturally. This satisfies
     * "stops receiving notifications" from the requirements; enforcing
     * "stops submitting proposals" server-side would require a revocation
     * list, which is out of scope unless explicitly requested.
     *
     * @param accountToken token identifying the session to log out
     * @throws InvalidTokenException if the token is malformed, invalid, or expired
     */
    void logout(String accountToken);

    /**
     * Updates a user's username and/or password. Both must be re-supplied
     * even if only one is changing, per {@code UpdateCredentialsRequest}.
     *
     * @param oldUsername current username; must exist
     * @param newUsername desired username; must not belong to a different existing account
     * @param oldPassword current password, verified against the stored hash
     * @param newPassword new password to hash and store
     * @return confirmation of the resulting username
     * @throws IncorrectPasswordException if {@code oldUsername} does not exist
     *         or {@code oldPassword} does not match the stored hash
     * @throws NewUsernameAlreadyTakenException if {@code newUsername} is already
     *         registered to a different account
     */
    UpdateCredentialsData updateCredentials(String oldUsername, String newUsername,
                                             String oldPassword, String newPassword);

    /**
     * Resolves the caller's identity from a signed account token. Called by
     * every other service before performing an authenticated operation.
     *
     * @param accountToken token supplied by the client
     * @return the identity and expiry encoded in the token
     * @throws InvalidTokenException if the token is malformed, invalid, or expired
     */
    AccountPrincipal resolve(String accountToken);
}