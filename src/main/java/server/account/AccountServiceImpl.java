package server.account;

import java.net.InetSocketAddress;
import java.util.Optional;
import server.account.exceptions.IncorrectPasswordException;
import server.account.exceptions.InvalidTokenException;
import server.account.exceptions.NewUsernameAlreadyTakenException;
import server.account.exceptions.UsernameAlreadyRegisteredException;
import server.dto.Account;
import server.dto.AccountPrincipal;
import server.dto.ServerConfig;
import shared.dto.LoginData;
import shared.dto.RegisterData;
import shared.dto.UpdateCredentialsData;

/**
 * Default implementation of {@link AccountService}. Uses constructor injection for all
 * dependencies.
 */
public record AccountServiceImpl(
    AccountRepository accountRepository,
    PasswordHasher passwordHasher,
    TokenSigner tokenSigner,
    NotificationRegistry notificationRegistry,
    ServerConfig config)
    implements AccountService {

  @Override
  public synchronized RegisterData register(String username, String password)
      throws UsernameAlreadyRegisteredException {
    if (accountRepository.existsByUsername(username)) {
      throw new UsernameAlreadyRegisteredException(username);
    }
    String hashed = passwordHasher.hash(password);
    accountRepository.save(new Account(username, hashed));
    return new RegisterData(username);
  }

  @Override
  public synchronized LoginData login(
      String username, String password, int udpPort, String remoteAddress)
      throws IncorrectPasswordException {
    Optional<Account> opt = accountRepository.findAccountByUsername(username);
    if (opt.isEmpty() || !passwordHasher.matches(password, opt.get().passwordHash())) {
      throw new IncorrectPasswordException(username);
    }

    long expiresAt = System.currentTimeMillis() + config.tokenExpiryMillis();
    String token = tokenSigner.sign(username, expiresAt);

    InetSocketAddress udpAddress = new InetSocketAddress(remoteAddress, udpPort);
    notificationRegistry.register(username, udpAddress);

    return new LoginData(token);
  }

  @Override
  public synchronized void logout(String accountToken) throws InvalidTokenException {
    AccountPrincipal principal = tokenSigner.verify(accountToken);
    notificationRegistry.unregister(principal.username());
  }

  @Override
  public synchronized UpdateCredentialsData updateCredentials(
      String oldUsername, String newUsername, String oldPassword, String newPassword)
      throws IncorrectPasswordException, NewUsernameAlreadyTakenException {
    Optional<Account> opt = accountRepository.findAccountByUsername(oldUsername);
    if (opt.isEmpty() || !passwordHasher.matches(oldPassword, opt.get().passwordHash())) {
      throw new IncorrectPasswordException(oldUsername);
    }

    Account current = opt.get();
    String updatedUsername = oldUsername;
    String updatedHash = current.passwordHash();

    if (newUsername != null && !newUsername.isEmpty() && !newUsername.equals(oldUsername)) {
      if (accountRepository.existsByUsername(newUsername)) {
        throw new NewUsernameAlreadyTakenException(newUsername);
      }
      updatedUsername = newUsername;
    }

    if (newPassword != null && !newPassword.isEmpty()) {
      updatedHash = passwordHasher.hash(newPassword);
    }

    Account updatedAccount = new Account(updatedUsername, updatedHash);
    accountRepository.save(updatedAccount);

    // If username changed, move the notification registration
    if (!updatedUsername.equals(oldUsername)) {
      accountRepository.deleteByUsername(oldUsername);

      final String finalUpdatedUsername = updatedUsername; // effectively final copy for lambda
      Optional<InetSocketAddress> udp = notificationRegistry.lookup(oldUsername);
      udp.ifPresent(
          addr -> {
            notificationRegistry.unregister(oldUsername);
            notificationRegistry.register(finalUpdatedUsername, addr);
          });
    }

    return new UpdateCredentialsData(updatedUsername);
  }

  @Override
  public AccountPrincipal resolve(String accountToken) throws InvalidTokenException {
    return tokenSigner.verify(accountToken);
  }
}
