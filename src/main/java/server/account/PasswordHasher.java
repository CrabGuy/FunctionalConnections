package server.account;

/**
 * Hashes and verifies passwords. Not part of the original Architecture
 * Reference — added here because {@link AccountService#register} and
 * {@link AccountService#updateCredentials} need something to turn a raw
 * password into the {@code passwordHash} stored on {@link server.dto.Account},
 * and no such component was specified. Flagging this addition rather than
 * folding hashing logic silently into {@code AccountService} itself.
 */
public interface PasswordHasher {

    /**
     * @param rawPassword plaintext password
     * @return a salted hash suitable for storage in {@link server.dto.Account#passwordHash()}
     */
    String hash(String rawPassword);

    /**
     * @param rawPassword plaintext password supplied at login/update time
     * @param passwordHash previously stored hash to check against
     * @return true if {@code rawPassword} hashes to {@code passwordHash}
     */
    boolean matches(String rawPassword, String passwordHash);
}