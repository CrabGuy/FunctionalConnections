package test.account;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import server.account.AccountRepository;
import server.account.AccountService;
import server.account.NotificationRegistry;
import server.account.PasswordHasher;
import server.account.TokenSigner;
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
 * Simple test runner for the Account slice. No external libraries required; uses plain assertions
 * and a factory for creating test instances, making tests robust to implementation renames.
 */
public class AccountTest {

  private static int passed = 0;
  private static int failed = 0;

  public static void main(String[] args) {
    System.out.println("Running Account slice tests...\n");

    runTest("testRegisterSuccess", AccountTest::testRegisterSuccess);
    runTest("testRegisterDuplicateUsername", AccountTest::testRegisterDuplicateUsername);
    runTest("testLoginSuccess", AccountTest::testLoginSuccess);
    runTest("testLoginIncorrectPassword", AccountTest::testLoginIncorrectPassword);
    runTest("testLoginNonexistentUsername", AccountTest::testLoginNonexistentUsername);
    runTest("testLogout", AccountTest::testLogout);
    runTest("testLogoutInvalidToken", AccountTest::testLogoutInvalidToken);
    runTest("testTokenInvalidAfterLogout", AccountTest::testTokenInvalidAfterLogout);
    runTest("testUpdateCredentialsSuccess", AccountTest::testUpdateCredentialsSuccess);
    runTest(
        "testUpdateCredentialsIncorrectOldPassword",
        AccountTest::testUpdateCredentialsIncorrectOldPassword);
    runTest(
        "testUpdateCredentialsNewUsernameTaken",
        AccountTest::testUpdateCredentialsNewUsernameTaken);
    runTest(
        "testUpdateCredentialsNonexistentOldUsername",
        AccountTest::testUpdateCredentialsNonexistentOldUsername);
    runTest("testUpdateCredentialsSameUsername", AccountTest::testUpdateCredentialsSameUsername);
    runTest("testResolveValidToken", AccountTest::testResolveValidToken);
    runTest("testResolveInvalidToken", AccountTest::testResolveInvalidToken);
    runTest("testPasswordHasher", AccountTest::testPasswordHasher);
    runTest("testTokenSigner", AccountTest::testTokenSigner);
    runTest("testTokenSignerTamperedSignature", AccountTest::testTokenSignerTamperedSignature);
    runTest("testTokenSignerNullToken", AccountTest::testTokenSignerNullToken);
    runTest("testAccountRepository", AccountTest::testAccountRepository);
    runTest(
        "testAccountRepositoryOverwritesExisting",
        AccountTest::testAccountRepositoryOverwritesExisting);
    runTest("testNotificationRegistry", AccountTest::testNotificationRegistry);
    runTest(
        "testNotificationRegistryReRegisterUpdatesAddress",
        AccountTest::testNotificationRegistryReRegisterUpdatesAddress);
    runTest("testConcurrentRegisterSameUsername", AccountTest::testConcurrentRegisterSameUsername);
    runTest("testConcurrentLoginLogoutSameUser", AccountTest::testConcurrentLoginLogoutSameUser);

    System.out.println("\n-----------------------------------");
    System.out.println("Tests passed: " + passed);
    System.out.println("Tests failed: " + failed);
    if (failed > 0) {
      System.exit(1);
    }
  }

  @FunctionalInterface
  private interface TestMethod {
    void run() throws Exception;
  }

  private static void runTest(String name, TestMethod test) {
    try {
      test.run();
      System.out.println("[PASS] " + name);
      passed++;
    } catch (Throwable t) {
      System.out.println("[FAIL] " + name + " - " + t.getMessage());
      failed++;
    }
  }

  // ---------------------- Helper assertion methods ----------------------

  private static void check(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }

  private static void assertThrows(
      Class<? extends Throwable> expectedType, Runnable action, String message) {
    try {
      action.run();
      throw new AssertionError(message + " (no exception thrown)");
    } catch (Throwable t) {
      if (!expectedType.isInstance(t)) {
        throw new AssertionError(
            message
                + " (expected "
                + expectedType.getSimpleName()
                + " but got "
                + t.getClass().getSimpleName()
                + ")");
      }
    }
  }

  // ---------------------- AccountService tests ----------------------

  private static void testRegisterSuccess() {
    AccountRepository repo = AccountTestFactory.createAccountRepository();
    NotificationRegistry notifReg = AccountTestFactory.createNotificationRegistry();
    PasswordHasher hasher = AccountTestFactory.createPasswordHasher();
    TokenSigner signer = AccountTestFactory.createTokenSigner();
    ServerConfig config = AccountTestFactory.createTestConfig();
    AccountService service =
        AccountTestFactory.createAccountService(repo, notifReg, hasher, signer, config);

    RegisterData data = service.register("alice", "password123");
    check("alice".equals(data.username()), "Register should return username");

    Optional<Account> stored = repo.findAccountByUsername("alice");
    check(stored.isPresent(), "Account should be stored");
    check(!stored.get().passwordHash().equals("password123"), "Raw password should not be stored");
    check(
        hasher.matches("password123", stored.get().passwordHash()),
        "Stored hash should match raw password");
  }

  private static void testRegisterDuplicateUsername() {
    AccountRepository repo = AccountTestFactory.createAccountRepository();
    NotificationRegistry notifReg = AccountTestFactory.createNotificationRegistry();
    PasswordHasher hasher = AccountTestFactory.createPasswordHasher();
    TokenSigner signer = AccountTestFactory.createTokenSigner();
    ServerConfig config = AccountTestFactory.createTestConfig();
    AccountService service =
        AccountTestFactory.createAccountService(repo, notifReg, hasher, signer, config);

    service.register("bob", "pw");
    assertThrows(
        UsernameAlreadyRegisteredException.class,
        () -> service.register("bob", "anotherpw"),
        "Duplicate username should throw UsernameAlreadyRegisteredException");
  }

  private static void testLoginSuccess() {
    AccountRepository repo = AccountTestFactory.createAccountRepository();
    NotificationRegistry notifReg = AccountTestFactory.createNotificationRegistry();
    PasswordHasher hasher = AccountTestFactory.createPasswordHasher();
    TokenSigner signer = AccountTestFactory.createTokenSigner();
    ServerConfig config = AccountTestFactory.createTestConfig();
    AccountService service =
        AccountTestFactory.createAccountService(repo, notifReg, hasher, signer, config);

    service.register("carol", "secret");
    InetSocketAddress udpAddr = new InetSocketAddress("127.0.0.1", 9999);
    LoginData data =
        service.login("carol", "secret", udpAddr.getPort(), udpAddr.getAddress().getHostAddress());

    check(
        data.accountToken() != null && !data.accountToken().isBlank(),
        "Login should return a non‑blank token");

    Optional<InetSocketAddress> registered = notifReg.lookup("carol");
    check(registered.isPresent(), "UDP address should be registered");
    check(udpAddr.equals(registered.get()), "Registered UDP address should match");
  }

  private static void testLoginIncorrectPassword() {
    AccountRepository repo = AccountTestFactory.createAccountRepository();
    NotificationRegistry notifReg = AccountTestFactory.createNotificationRegistry();
    PasswordHasher hasher = AccountTestFactory.createPasswordHasher();
    TokenSigner signer = AccountTestFactory.createTokenSigner();
    ServerConfig config = AccountTestFactory.createTestConfig();
    AccountService service =
        AccountTestFactory.createAccountService(repo, notifReg, hasher, signer, config);

    service.register("dave", "correct");
    assertThrows(
        IncorrectPasswordException.class,
        () -> service.login("dave", "wrong", 0, "127.0.0.1"),
        "Login with wrong password should throw IncorrectPasswordException");
  }

  private static void testLoginNonexistentUsername() {
    AccountRepository repo = AccountTestFactory.createAccountRepository();
    NotificationRegistry notifReg = AccountTestFactory.createNotificationRegistry();
    PasswordHasher hasher = AccountTestFactory.createPasswordHasher();
    TokenSigner signer = AccountTestFactory.createTokenSigner();
    ServerConfig config = AccountTestFactory.createTestConfig();
    AccountService service =
        AccountTestFactory.createAccountService(repo, notifReg, hasher, signer, config);

    assertThrows(
        IncorrectPasswordException.class,
        () -> service.login("ghost", "whatever", 0, "127.0.0.1"),
        "Login with an unknown username should throw IncorrectPasswordException, not leak account"
            + " existence");
  }

  private static void testLogout() {
    AccountRepository repo = AccountTestFactory.createAccountRepository();
    NotificationRegistry notifReg = AccountTestFactory.createNotificationRegistry();
    PasswordHasher hasher = AccountTestFactory.createPasswordHasher();
    TokenSigner signer = AccountTestFactory.createTokenSigner();
    ServerConfig config = AccountTestFactory.createTestConfig();
    AccountService service =
        AccountTestFactory.createAccountService(repo, notifReg, hasher, signer, config);

    service.register("erin", "pw");
    LoginData data = service.login("erin", "pw", 12345, "127.0.0.1");
    check(notifReg.lookup("erin").isPresent(), "UDP should be registered after login");

    service.logout(data.accountToken());
    check(notifReg.lookup("erin").isEmpty(), "UDP registration should be removed after logout");
  }

  private static void testLogoutInvalidToken() {
    AccountRepository repo = AccountTestFactory.createAccountRepository();
    NotificationRegistry notifReg = AccountTestFactory.createNotificationRegistry();
    PasswordHasher hasher = AccountTestFactory.createPasswordHasher();
    TokenSigner signer = AccountTestFactory.createTokenSigner();
    ServerConfig config = AccountTestFactory.createTestConfig();
    AccountService service =
        AccountTestFactory.createAccountService(repo, notifReg, hasher, signer, config);

    assertThrows(
        InvalidTokenException.class,
        () -> service.logout("not-a-real-token"),
        "Logout with an invalid/unknown token should throw InvalidTokenException");
  }

  private static void testTokenInvalidAfterLogout() {
    AccountRepository repo = AccountTestFactory.createAccountRepository();
    NotificationRegistry notifReg = AccountTestFactory.createNotificationRegistry();
    PasswordHasher hasher = AccountTestFactory.createPasswordHasher();
    TokenSigner signer = AccountTestFactory.createTokenSigner();
    ServerConfig config = AccountTestFactory.createTestConfig();
    AccountService service =
        AccountTestFactory.createAccountService(repo, notifReg, hasher, signer, config);

    service.register("fiona", "pw");
    LoginData data = service.login("fiona", "pw", 12345, "127.0.0.1");

    // Token is valid before logout
    AccountPrincipal principal = service.resolve(data.accountToken());
    check("fiona".equals(principal.username()), "Token should be resolvable before logout");

    service.logout(data.accountToken());

    // After logout, the token must be invalid
    assertThrows(
        InvalidTokenException.class,
        () -> service.resolve(data.accountToken()),
        "Token should be invalid after logout");
  }

  private static void testUpdateCredentialsSuccess() {
    AccountRepository repo = AccountTestFactory.createAccountRepository();
    NotificationRegistry notifReg = AccountTestFactory.createNotificationRegistry();
    PasswordHasher hasher = AccountTestFactory.createPasswordHasher();
    TokenSigner signer = AccountTestFactory.createTokenSigner();
    ServerConfig config = AccountTestFactory.createTestConfig();
    AccountService service =
        AccountTestFactory.createAccountService(repo, notifReg, hasher, signer, config);

    service.register("frank", "oldpass");
    UpdateCredentialsData result =
        service.updateCredentials("frank", "frank2", "oldpass", "newpass");

    check("frank2".equals(result.newUsername()), "Update should return new username");
    check(repo.findAccountByUsername("frank").isEmpty(), "Old username should no longer exist");
    Optional<Account> updated = repo.findAccountByUsername("frank2");
    check(updated.isPresent(), "New username should exist");
    check(
        hasher.matches("newpass", updated.get().passwordHash()),
        "Password should be updated to new hash");
  }

  private static void testUpdateCredentialsIncorrectOldPassword() {
    AccountRepository repo = AccountTestFactory.createAccountRepository();
    NotificationRegistry notifReg = AccountTestFactory.createNotificationRegistry();
    PasswordHasher hasher = AccountTestFactory.createPasswordHasher();
    TokenSigner signer = AccountTestFactory.createTokenSigner();
    ServerConfig config = AccountTestFactory.createTestConfig();
    AccountService service =
        AccountTestFactory.createAccountService(repo, notifReg, hasher, signer, config);

    service.register("grace", "right");
    assertThrows(
        IncorrectPasswordException.class,
        () -> service.updateCredentials("grace", "grace2", "wrong", "newpass"),
        "Update with wrong old password should throw IncorrectPasswordException");
  }

  private static void testUpdateCredentialsNewUsernameTaken() {
    AccountRepository repo = AccountTestFactory.createAccountRepository();
    NotificationRegistry notifReg = AccountTestFactory.createNotificationRegistry();
    PasswordHasher hasher = AccountTestFactory.createPasswordHasher();
    TokenSigner signer = AccountTestFactory.createTokenSigner();
    ServerConfig config = AccountTestFactory.createTestConfig();
    AccountService service =
        AccountTestFactory.createAccountService(repo, notifReg, hasher, signer, config);

    service.register("henry", "pw1");
    service.register("irene", "pw2");
    assertThrows(
        NewUsernameAlreadyTakenException.class,
        () -> service.updateCredentials("henry", "irene", "pw1", "pw3"),
        "Update to an already taken username should throw NewUsernameAlreadyTakenException");
  }

  private static void testUpdateCredentialsNonexistentOldUsername() {
    AccountRepository repo = AccountTestFactory.createAccountRepository();
    NotificationRegistry notifReg = AccountTestFactory.createNotificationRegistry();
    PasswordHasher hasher = AccountTestFactory.createPasswordHasher();
    TokenSigner signer = AccountTestFactory.createTokenSigner();
    ServerConfig config = AccountTestFactory.createTestConfig();
    AccountService service =
        AccountTestFactory.createAccountService(repo, notifReg, hasher, signer, config);

    assertThrows(
        IncorrectPasswordException.class,
        () -> service.updateCredentials("ghost", "ghost2", "whatever", "newpw"),
        "updateCredentials for a never-registered username should throw"
            + " IncorrectPasswordException");
  }

  private static void testUpdateCredentialsSameUsername() {
    AccountRepository repo = AccountTestFactory.createAccountRepository();
    NotificationRegistry notifReg = AccountTestFactory.createNotificationRegistry();
    PasswordHasher hasher = AccountTestFactory.createPasswordHasher();
    TokenSigner signer = AccountTestFactory.createTokenSigner();
    ServerConfig config = AccountTestFactory.createTestConfig();
    AccountService service =
        AccountTestFactory.createAccountService(repo, notifReg, hasher, signer, config);

    service.register("nora", "oldpass");

    UpdateCredentialsData result = service.updateCredentials("nora", "nora", "oldpass", "newpass");
    check("nora".equals(result.newUsername()), "Username should remain unchanged");

    Optional<Account> updated = repo.findAccountByUsername("nora");
    check(updated.isPresent(), "Account should still exist under the same username");
    check(
        hasher.matches("newpass", updated.get().passwordHash()),
        "Password should be updated even though the username didn't change");
  }

  private static void testResolveValidToken() {
    AccountRepository repo = AccountTestFactory.createAccountRepository();
    NotificationRegistry notifReg = AccountTestFactory.createNotificationRegistry();
    PasswordHasher hasher = AccountTestFactory.createPasswordHasher();
    TokenSigner signer = AccountTestFactory.createTokenSigner();
    ServerConfig config = AccountTestFactory.createTestConfig();
    AccountService service =
        AccountTestFactory.createAccountService(repo, notifReg, hasher, signer, config);

    service.register("jack", "pw");
    LoginData data = service.login("jack", "pw", 0, "127.0.0.1");
    AccountPrincipal principal = service.resolve(data.accountToken());
    check("jack".equals(principal.username()), "Resolved principal should have correct username");
    check(principal.expiresAt() > Instant.now().getEpochSecond(), "Token should not be expired");
  }

  private static void testResolveInvalidToken() {
    AccountRepository repo = AccountTestFactory.createAccountRepository();
    NotificationRegistry notifReg = AccountTestFactory.createNotificationRegistry();
    PasswordHasher hasher = AccountTestFactory.createPasswordHasher();
    TokenSigner signer = AccountTestFactory.createTokenSigner();
    ServerConfig config = AccountTestFactory.createTestConfig();
    AccountService service =
        AccountTestFactory.createAccountService(repo, notifReg, hasher, signer, config);

    assertThrows(
        InvalidTokenException.class,
        () -> service.resolve("invalid-token"),
        "Resolving an invalid token should throw InvalidTokenException");
  }

  // ---------------------- Component‑level tests ----------------------

  private static void testPasswordHasher() {
    PasswordHasher hasher = AccountTestFactory.createPasswordHasher();
    String raw = "mySecret123";
    String hash = hasher.hash(raw);
    check(!raw.equals(hash), "Hash should not equal raw password");
    check(hasher.matches(raw, hash), "Correct password should match hash");
    check(!hasher.matches("wrong", hash), "Wrong password should not match hash");
  }

  private static void testTokenSigner() {
    TokenSigner signer = AccountTestFactory.createTokenSigner();
    long nowMilli = System.currentTimeMillis();
    String token = signer.sign("kate", nowMilli + 60 * 1000);
    check(token != null && !token.isBlank(), "Token should be non-blank");
    AccountPrincipal principal = signer.verify(token);
    check("kate".equals(principal.username()), "Verified token should contain username");
    check(principal.expiresAt() > Instant.now().getEpochSecond(), "Token should be valid");

    String expiredToken = signer.sign("kate", nowMilli - 10 * 1000);
    assertThrows(
        InvalidTokenException.class,
        () -> signer.verify(expiredToken),
        "Verifying an expired token should throw InvalidTokenException");
  }

  private static void testTokenSignerTamperedSignature() {
    TokenSigner signer = AccountTestFactory.createTokenSigner();
    long nowSeconds = Instant.now().getEpochSecond();
    String token = signer.sign("oliver", nowSeconds + 60);

    String tampered = flipMiddleCharacter(token);
    check(
        !tampered.equals(token),
        "Test setup sanity check: tampering must actually change the token");

    assertThrows(
        InvalidTokenException.class,
        () -> signer.verify(tampered),
        "Verifying a token with a tampered signature/payload should throw InvalidTokenException");
  }

  private static void testTokenSignerNullToken() {
    TokenSigner signer = AccountTestFactory.createTokenSigner();
    assertThrows(
        InvalidTokenException.class,
        () -> signer.verify(null),
        "Verifying a null token should throw InvalidTokenException, not NullPointerException");
  }

  private static String flipMiddleCharacter(String token) {
    char[] chars = token.toCharArray();
    int idx = chars.length / 2;
    chars[idx] = (chars[idx] == 'A') ? 'B' : 'A';
    return new String(chars);
  }

  private static void testAccountRepository() {
    AccountRepository repo = AccountTestFactory.createAccountRepository();
    Account acc = new Account("lisa", "hash123");
    repo.save(acc);
    check(repo.existsByUsername("lisa"), "Repository should report existing username");
    Optional<Account> found = repo.findAccountByUsername("lisa");
    check(found.isPresent(), "Repository should find saved account");
    check("hash123".equals(found.get().passwordHash()), "Found account should have correct hash");
    check(!repo.existsByUsername("nobody"), "Repository should not report non‑existing username");
  }

  private static void testAccountRepositoryOverwritesExisting() {
    AccountRepository repo = AccountTestFactory.createAccountRepository();
    repo.save(new Account("liam", "hashV1"));
    check(
        "hashV1".equals(repo.findAccountByUsername("liam").get().passwordHash()),
        "Sanity check on initial save");

    repo.save(new Account("liam", "hashV2"));
    Optional<Account> after = repo.findAccountByUsername("liam");
    check(after.isPresent(), "Account should still be present after overwrite");
    check(
        "hashV2".equals(after.get().passwordHash()),
        "Saving an existing username should overwrite the stored account, not duplicate it");
  }

  private static void testNotificationRegistry() {
    NotificationRegistry reg = AccountTestFactory.createNotificationRegistry();
    InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 7777);
    reg.register("mike", addr);
    Optional<InetSocketAddress> lookedUp = reg.lookup("mike");
    check(lookedUp.isPresent(), "Registry should find registered user");
    check(addr.equals(lookedUp.get()), "Registry should return correct address");
    check(
        reg.lookup("not-registered").isEmpty(),
        "Registry should return empty for non‑registered user");

    reg.unregister("mike");
    check(reg.lookup("mike").isEmpty(), "Registry should remove user after unregister");
  }

  private static void testNotificationRegistryReRegisterUpdatesAddress() {
    NotificationRegistry reg = AccountTestFactory.createNotificationRegistry();
    InetSocketAddress first = new InetSocketAddress("127.0.0.1", 1111);
    InetSocketAddress second = new InetSocketAddress("127.0.0.1", 2222);

    reg.register("nina", first);
    reg.register("nina", second);

    Optional<InetSocketAddress> looked = reg.lookup("nina");
    check(looked.isPresent(), "Registry should find the re-registered user");
    check(
        second.equals(looked.get()),
        "Re-registering (e.g. re-login without an intervening logout) should replace the previous"
            + " UDP address");
  }

  // ---------------------- Concurrency tests ----------------------
  // These exist because GUIDELINES.md/ROADMAP.md require synchronized data
  // structures for a multithreaded server: a naive check-then-act
  // implementation can pass every single-threaded test above and still be
  // unsafe under real concurrent access.

  private static void testConcurrentRegisterSameUsername() throws InterruptedException {
    AccountRepository repo = AccountTestFactory.createAccountRepository();
    NotificationRegistry notifReg = AccountTestFactory.createNotificationRegistry();
    PasswordHasher hasher = AccountTestFactory.createPasswordHasher();
    TokenSigner signer = AccountTestFactory.createTokenSigner();
    ServerConfig config = AccountTestFactory.createTestConfig();
    AccountService service =
        AccountTestFactory.createAccountService(repo, notifReg, hasher, signer, config);

    int threadCount = 20;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startGate = new CountDownLatch(1);
    CountDownLatch doneGate = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger();
    AtomicInteger duplicateRejectionCount = new AtomicInteger();
    List<Throwable> unexpected = Collections.synchronizedList(new ArrayList<>());

    for (int i = 0; i < threadCount; i++) {
      int idx = i;
      executor.submit(
          () -> {
            try {
              startGate.await();
              service.register("racer", "pw" + idx);
              successCount.incrementAndGet();
            } catch (UsernameAlreadyRegisteredException expected) {
              duplicateRejectionCount.incrementAndGet();
            } catch (Throwable t) {
              unexpected.add(t);
            } finally {
              doneGate.countDown();
            }
          });
    }

    startGate.countDown();
    boolean finished = doneGate.await(10, TimeUnit.SECONDS);
    executor.shutdownNow();

    check(finished, "All registration threads should complete within the timeout");
    check(
        unexpected.isEmpty(), "No unexpected exceptions during concurrent register: " + unexpected);
    check(
        successCount.get() == 1,
        "Exactly one concurrent register(\"racer\", ...) call should succeed, got "
            + successCount.get()
            + " (a check-then-act race would let more than one through)");
    check(
        duplicateRejectionCount.get() == threadCount - 1,
        "Every other concurrent register(\"racer\", ...) call should throw"
            + " UsernameAlreadyRegisteredException, got "
            + duplicateRejectionCount.get());
    check(repo.existsByUsername("racer"), "Repository should contain the winning account");
  }

  private static void testConcurrentLoginLogoutSameUser() throws InterruptedException {
    AccountRepository repo = AccountTestFactory.createAccountRepository();
    NotificationRegistry notifReg = AccountTestFactory.createNotificationRegistry();
    PasswordHasher hasher = AccountTestFactory.createPasswordHasher();
    TokenSigner signer = AccountTestFactory.createTokenSigner();
    ServerConfig config = AccountTestFactory.createTestConfig();
    AccountService service =
        AccountTestFactory.createAccountService(repo, notifReg, hasher, signer, config);

    service.register("oscar", "pw");

    int threadCount = 20;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startGate = new CountDownLatch(1);
    CountDownLatch doneGate = new CountDownLatch(threadCount);
    List<Throwable> unexpected = Collections.synchronizedList(new ArrayList<>());

    for (int i = 0; i < threadCount; i++) {
      int port = 10000 + i;
      executor.submit(
          () -> {
            try {
              startGate.await();
              LoginData data = service.login("oscar", "pw", port, "127.0.0.1");
              service.logout(data.accountToken());
            } catch (Throwable t) {
              unexpected.add(t);
            } finally {
              doneGate.countDown();
            }
          });
    }

    startGate.countDown();
    boolean finished = doneGate.await(10, TimeUnit.SECONDS);
    executor.shutdownNow();

    check(finished, "All login/logout threads should complete within the timeout");
    check(
        unexpected.isEmpty(),
        "No exceptions expected from concurrent login/logout of the same user: " + unexpected);
    check(
        notifReg.lookup("oscar").isEmpty(),
        "NotificationRegistry should end up empty after every concurrent login was followed by its"
            + " own logout");
  }
}
