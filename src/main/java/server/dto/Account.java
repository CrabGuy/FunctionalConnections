package server.dto;

/**
 * A registered player account. {@code passwordHash} is never the raw
 * password — hashing is the responsibility of whatever supplies this
 * record to the repository (see {@link server.account.PasswordHasher}).
 */
public record Account(String username, String passwordHash) {
}