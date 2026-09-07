package test.account;

import server.account.AccountRepository;
import server.account.AccountService;
import server.account.AccountServiceImpl;
import server.account.InMemoryAccountRepository;
import server.account.InMemoryNotificationRegistry;
import server.account.JwtTokenSigner;
import server.account.NotificationRegistry;
import server.account.PasswordHasher;
import server.account.Sha256PasswordHasher;
import server.account.TokenSigner;
import server.dto.ServerConfig;
import server.game.InMemoryPlayerGameRepository;
import server.game.PlayerGameRepository;

public class AccountTestFactory {

    private static final ServerConfig DEFAULT_CONFIG =
            new ServerConfig(
                    5000,
                    300_000,
                    "storage",
                    60_000,
                    "test-secret-key-for-jwt-signing-which-is-long-enough",
                    3_600_000,
                    4,
                    "games.json");

    public static ServerConfig createTestConfig() {
        return new ServerConfig(
                DEFAULT_CONFIG.tcpPort(),
                DEFAULT_CONFIG.gameDurationMillis(),
                DEFAULT_CONFIG.storageDirectory(),
                DEFAULT_CONFIG.persistenceIntervalMillis(),
                DEFAULT_CONFIG.jwtSecret(),
                DEFAULT_CONFIG.tokenExpiryMillis(),
                DEFAULT_CONFIG.threadPoolSize(),
                DEFAULT_CONFIG.gameDataFile());
    }

    public static AccountRepository createAccountRepository() {
        return new InMemoryAccountRepository();
    }

    public static NotificationRegistry createNotificationRegistry() {
        return new InMemoryNotificationRegistry();
    }

    public static PasswordHasher createPasswordHasher() {
        return new Sha256PasswordHasher();
    }

    public static TokenSigner createTokenSigner() {
        return new JwtTokenSigner(DEFAULT_CONFIG.jwtSecret());
    }

    public static TokenSigner createTokenSigner(String secret) {
        return new JwtTokenSigner(secret);
    }

    // Full constructor (new signature)
    public static AccountService createAccountService(
            AccountRepository repo,
            PlayerGameRepository playerGameRepo,
            NotificationRegistry notifReg,
            PasswordHasher hasher,
            TokenSigner signer,
            ServerConfig config) {
        return new AccountServiceImpl(repo, playerGameRepo, hasher, signer, notifReg, config);
    }

    // Overload for backward compatibility with old tests that do not provide PlayerGameRepository
    public static AccountService createAccountService(
            AccountRepository repo,
            NotificationRegistry notifReg,
            PasswordHasher hasher,
            TokenSigner signer,
            ServerConfig config) {
        return createAccountService(
                repo,
                createPlayerGameRepository(),
                notifReg,
                hasher,
                signer,
                config);
    }

    public static AccountService createDefaultAccountService() {
        return createAccountService(
                createAccountRepository(),
                createNotificationRegistry(),
                createPasswordHasher(),
                createTokenSigner(),
                DEFAULT_CONFIG);
    }

    private static PlayerGameRepository createPlayerGameRepository() {
        return new InMemoryPlayerGameRepository();
    }
}