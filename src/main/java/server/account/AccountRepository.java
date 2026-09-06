package server.account;

import java.util.List;
import java.util.Optional;
import server.dto.Account;

public interface AccountRepository {
  Optional<Account> findAccountByUsername(String username);

  void save(Account account);

  boolean existsByUsername(String username);

  void deleteByUsername(String username);

  List<Account> findAll();
}
