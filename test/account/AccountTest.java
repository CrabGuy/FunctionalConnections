package test.account;

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

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Optional;

/**
 * Simple test runner for the Account slice.
 * No external libraries required; uses plain assertions and a factory
 * for creating test instances, making tests robust to implementation renames.
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
        runTest("testLogout", AccountTest::testLogout);
        runTest("testUpdateCredentialsSuccess", AccountTest::testUpdateCredentialsSuccess);
        runTest("testUpdateCredentialsIncorrectOldPassword", AccountTest::testUpdateCredentialsIncorrectOldPassword);
        runTest("testUpdateCredentialsNewUsernameTaken", AccountTest::testUpdateCredentialsNewUsernameTaken);
        runTest("testResolveValidToken", AccountTest::testResolveValidToken);
        runTest("testResolveInvalidToken", AccountTest::testResolveInvalidToken);
        runTest("testPasswordHasher", AccountTest::testPasswordHasher);
        runTest("testTokenSigner", AccountTest::testTokenSigner);
        runTest("testAccountRepository", AccountTest::testAccountRepository);
        runTest("testNotificationRegistry", AccountTest::testNotificationRegistry);

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

    private static void assertThrows(Class<? extends Throwable> expectedType, Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message + " (no exception thrown)");
        } catch (Throwable t) {
            if (!expectedType.isInstance(t)) {
                throw new AssertionError(message + " (expected " + expectedType.getSimpleName()
                        + " but got " + t.getClass().getSimpleName() + ")");
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
        AccountService service = AccountTestFactory.createAccountService(repo, notifReg, hasher, signer, config);

        RegisterData data = service.register("alice", "password123");
        check("alice".equals(data.username()), "Register should return username");

        Optional<Account> stored = repo.findAccountByUsername("alice");
        check(stored.isPresent(), "Account should be stored");
        check(!stored.get().passwordHash().equals("password123"),
                "Raw password should not be stored");
        check(hasher.matches("password123", stored.get().passwordHash()),
                "Stored hash should match raw password");
    }

    private static void testRegisterDuplicateUsername() {
        AccountRepository repo = AccountTestFactory.createAccountRepository();
        NotificationRegistry notifReg = AccountTestFactory.createNotificationRegistry();
        PasswordHasher hasher = AccountTestFactory.createPasswordHasher();
        TokenSigner signer = AccountTestFactory.createTokenSigner();
        ServerConfig config = AccountTestFactory.createTestConfig();
        AccountService service = AccountTestFactory.createAccountService(repo, notifReg, hasher, signer, config);

        service.register("bob", "pw");
        assertThrows(UsernameAlreadyRegisteredException.class,
                () -> service.register("bob", "anotherpw"),
                "Duplicate username should throw UsernameAlreadyRegisteredException");
    }

    private static void testLoginSuccess() {
        AccountRepository repo = AccountTestFactory.createAccountRepository();
        NotificationRegistry notifReg = AccountTestFactory.createNotificationRegistry();
        PasswordHasher hasher = AccountTestFactory.createPasswordHasher();
        TokenSigner signer = AccountTestFactory.createTokenSigner();
        ServerConfig config = AccountTestFactory.createTestConfig();
        AccountService service = AccountTestFactory.createAccountService(repo, notifReg, hasher, signer, config);

        service.register("carol", "secret");
        InetSocketAddress udpAddr = new InetSocketAddress("127.0.0.1", 9999);
        LoginData data = service.login("carol", "secret", udpAddr.getPort(), udpAddr.getAddress().getHostAddress());

        check(data.accountToken() != null && !data.accountToken().isBlank(),
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
        AccountService service = AccountTestFactory.createAccountService(repo, notifReg, hasher, signer, config);

        service.register("dave", "correct");
        assertThrows(IncorrectPasswordException.class,
                () -> service.login("dave", "wrong", 0, "127.0.0.1"),
                "Login with wrong password should throw IncorrectPasswordException");
    }

    private static void testLogout() {
        AccountRepository repo = AccountTestFactory.createAccountRepository();
        NotificationRegistry notifReg = AccountTestFactory.createNotificationRegistry();
        PasswordHasher hasher = AccountTestFactory.createPasswordHasher();
        TokenSigner signer = AccountTestFactory.createTokenSigner();
        ServerConfig config = AccountTestFactory.createTestConfig();
        AccountService service = AccountTestFactory.createAccountService(repo, notifReg, hasher, signer, config);

        service.register("erin", "pw");
        LoginData data = service.login("erin", "pw", 12345, "127.0.0.1");
        check(notifReg.lookup("erin").isPresent(), "UDP should be registered after login");

        service.logout(data.accountToken());
        check(notifReg.lookup("erin").isEmpty(), "UDP registration should be removed after logout");
    }

    private static void testUpdateCredentialsSuccess() {
        AccountRepository repo = AccountTestFactory.createAccountRepository();
        NotificationRegistry notifReg = AccountTestFactory.createNotificationRegistry();
        PasswordHasher hasher = AccountTestFactory.createPasswordHasher();
        TokenSigner signer = AccountTestFactory.createTokenSigner();
        ServerConfig config = AccountTestFactory.createTestConfig();
        AccountService service = AccountTestFactory.createAccountService(repo, notifReg, hasher, signer, config);

        service.register("frank", "oldpass");
        UpdateCredentialsData result = service.updateCredentials("frank", "frank2", "oldpass", "newpass");

        check("frank2".equals(result.newUsername()), "Update should return new username");
        check(repo.findAccountByUsername("frank").isEmpty(), "Old username should no longer exist");
        Optional<Account> updated = repo.findAccountByUsername("frank2");
        check(updated.isPresent(), "New username should exist");
        check(hasher.matches("newpass", updated.get().passwordHash()),
                "Password should be updated to new hash");
    }

    private static void testUpdateCredentialsIncorrectOldPassword() {
        AccountRepository repo = AccountTestFactory.createAccountRepository();
        NotificationRegistry notifReg = AccountTestFactory.createNotificationRegistry();
        PasswordHasher hasher = AccountTestFactory.createPasswordHasher();
        TokenSigner signer = AccountTestFactory.createTokenSigner();
        ServerConfig config = AccountTestFactory.createTestConfig();
        AccountService service = AccountTestFactory.createAccountService(repo, notifReg, hasher, signer, config);

        service.register("grace", "right");
        assertThrows(IncorrectPasswordException.class,
                () -> service.updateCredentials("grace", "grace2", "wrong", "newpass"),
                "Update with wrong old password should throw IncorrectPasswordException");
    }

    private static void testUpdateCredentialsNewUsernameTaken() {
        AccountRepository repo = AccountTestFactory.createAccountRepository();
        NotificationRegistry notifReg = AccountTestFactory.createNotificationRegistry();
        PasswordHasher hasher = AccountTestFactory.createPasswordHasher();
        TokenSigner signer = AccountTestFactory.createTokenSigner();
        ServerConfig config = AccountTestFactory.createTestConfig();
        AccountService service = AccountTestFactory.createAccountService(repo, notifReg, hasher, signer, config);

        service.register("henry", "pw1");
        service.register("irene", "pw2");
        assertThrows(NewUsernameAlreadyTakenException.class,
                () -> service.updateCredentials("henry", "irene", "pw1", "pw3"),
                "Update to an already taken username should throw NewUsernameAlreadyTakenException");
    }

    private static void testResolveValidToken() {
        AccountRepository repo = AccountTestFactory.createAccountRepository();
        NotificationRegistry notifReg = AccountTestFactory.createNotificationRegistry();
        PasswordHasher hasher = AccountTestFactory.createPasswordHasher();
        TokenSigner signer = AccountTestFactory.createTokenSigner();
        ServerConfig config = AccountTestFactory.createTestConfig();
        AccountService service = AccountTestFactory.createAccountService(repo, notifReg, hasher, signer, config);

        service.register("jack", "pw");
        LoginData data = service.login("jack", "pw", 0, "127.0.0.1");
        AccountPrincipal principal = service.resolve(data.accountToken());
        check("jack".equals(principal.username()), "Resolved principal should have correct username");
        check(principal.expiresAt() > Instant.now().getEpochSecond(),
                "Token should not be expired");
    }

    private static void testResolveInvalidToken() {
        AccountRepository repo = AccountTestFactory.createAccountRepository();
        NotificationRegistry notifReg = AccountTestFactory.createNotificationRegistry();
        PasswordHasher hasher = AccountTestFactory.createPasswordHasher();
        TokenSigner signer = AccountTestFactory.createTokenSigner();
        ServerConfig config = AccountTestFactory.createTestConfig();
        AccountService service = AccountTestFactory.createAccountService(repo, notifReg, hasher, signer, config);

        assertThrows(InvalidTokenException.class,
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
        long nowSeconds = Instant.now().getEpochSecond();
        String token = signer.sign("kate", nowSeconds + 60);
        check(token != null && !token.isBlank(), "Token should be non‑blank");
        AccountPrincipal principal = signer.verify(token);
        check("kate".equals(principal.username()), "Verified token should contain username");
        check(principal.expiresAt() > Instant.now().getEpochSecond(), "Token should be valid");

        // Expired token
        String expiredToken = signer.sign("kate", nowSeconds - 10);
        assertThrows(InvalidTokenException.class,
                () -> signer.verify(expiredToken),
                "Verifying an expired token should throw InvalidTokenException");
    }

    private static void testAccountRepository() {
        AccountRepository repo = AccountTestFactory.createAccountRepository();
        Account acc = new Account("lisa", "hash123");
        repo.save(acc);
        check(repo.existsByUsername("lisa"), "Repository should report existing username");
        Optional<Account> found = repo.findAccountByUsername("lisa");
        check(found.isPresent(), "Repository should find saved account");
        check("hash123".equals(found.get().passwordHash()),
                "Found account should have correct hash");
        check(!repo.existsByUsername("nobody"), "Repository should not report non‑existing username");
    }

    private static void testNotificationRegistry() {
        NotificationRegistry reg = AccountTestFactory.createNotificationRegistry();
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 7777);
        reg.register("mike", addr);
        Optional<InetSocketAddress> lookedUp = reg.lookup("mike");
        check(lookedUp.isPresent(), "Registry should find registered user");
        check(addr.equals(lookedUp.get()), "Registry should return correct address");
        check(reg.lookup("not-registered").isEmpty(),
                "Registry should return empty for non‑registered user");

        reg.unregister("mike");
        check(reg.lookup("mike").isEmpty(), "Registry should remove user after unregister");
    }
}