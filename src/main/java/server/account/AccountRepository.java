package server.account;

import java.util.List;
import java.util.Optional;
import server.dto.Account;

/** Repository interface for managing {@link Account} entities. */
public interface AccountRepository {

  /**
   * Finds an account by username.
   *
   * @param username the username
   * @return an Optional containing the account if found, otherwise empty
   */
  Optional<Account> findAccountByUsername(String username);

  /**
   * Saves an account (insert or update).
   *
   * @param account the account to save
   */
  void save(Account account);

  /**
   * Checks if an account with the given username exists.
   *
   * @param username the username
   * @return true if exists, false otherwise
   */
  boolean existsByUsername(String username);

  /**
   * Deletes an account by username.
   *
   * @param username the username
   */
  void deleteByUsername(String username);

  /**
   * Returns all accounts.
   *
   * @return a list of all accounts
   */
  List<Account> findAll();
}
