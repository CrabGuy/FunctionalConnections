package server.account;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import server.dto.Account;

/**
 * In-memory implementation of {@link AccountRepository} using a concurrent map keyed by username.
 */
public class InMemoryAccountRepository implements AccountRepository {

  private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();

  /** {@inheritDoc} */
  @Override
  public Optional<Account> findAccountByUsername(String username) {
    return Optional.ofNullable(accounts.get(username));
  }

  /** {@inheritDoc} */
  @Override
  public void save(Account account) {
    accounts.put(account.username(), account);
  }

  /** {@inheritDoc} */
  @Override
  public boolean existsByUsername(String username) {
    return accounts.containsKey(username);
  }

  /** {@inheritDoc} */
  @Override
  public void deleteByUsername(String username) {
    accounts.remove(username);
  }

  /** {@inheritDoc} */
  @Override
  public List<Account> findAll() {
    return List.copyOf(accounts.values());
  }
}
