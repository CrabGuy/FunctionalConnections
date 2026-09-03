package server.account;

import server.dto.Account;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryAccountRepository implements AccountRepository {

    private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();

    @Override
    public Optional<Account> findAccountByUsername(String username) {
        return Optional.ofNullable(accounts.get(username));
    }

    @Override
    public void save(Account account) {
        accounts.put(account.username(), account);
    }

    @Override
    public boolean existsByUsername(String username) {
        return accounts.containsKey(username);
    }

    @Override
    public void deleteByUsername(String username) {
        accounts.remove(username);
    }

    @Override
    public List<Account> findAll() {
        return List.copyOf(accounts.values());
    }
}