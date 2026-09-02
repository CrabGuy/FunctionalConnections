package server.account;
import server.dto.Account;
import java.util.Optional;
public interface AccountRepository {
    Optional<Account> findAccountByUsername(String username);
    void save(Account account);
    boolean existsByUsername(String username);
}
