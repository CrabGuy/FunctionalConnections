package server.account;

import server.dto.Account;

import java.util.Optional;

/**
 * Storage boundary for {@link Account} records. Implementations must be
 * thread-safe — accessed concurrently from the server's connection
 * thread pool.
 */
public interface AccountRepository {

    Optional<Account> findByUsername(String username);

    void save(Account account);

    boolean existsByUsername(String username);
}