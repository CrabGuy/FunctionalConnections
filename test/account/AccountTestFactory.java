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

/**
 * Factory for creating test instances of the account components.
 * Centralises concrete class names and configuration so that renames or
 * constructor changes require updates only here.
 */
public class AccountTestFactory {

    private static final ServerConfig DEFAULT_CONFIG = new ServerConfig(
            5000,                // tcpPort
            300_000,             // gameDurationMillis
            "storage",           // storageDirectory
            60_000,              // persistenceIntervalMillis
            "test-secret-key-for-jwt-signing-which-is-long-enough",
            3_600_000,           // tokenExpiryMillis (not used by signer directly)
            4,                    // threadPoolSize
            "games.json"
    );

    public static ServerConfig createTestConfig() {
        // Return a copy to avoid accidental mutation
        return new ServerConfig(
                DEFAULT_CONFIG.tcpPort(),
                DEFAULT_CONFIG.gameDurationMillis(),
                DEFAULT_CONFIG.storageDirectory(),
                DEFAULT_CONFIG.persistenceIntervalMillis(),
                DEFAULT_CONFIG.jwtSecret(),
                DEFAULT_CONFIG.tokenExpiryMillis(),
                DEFAULT_CONFIG.threadPoolSize(),
                DEFAULT_CONFIG.gameDataFile()
        );
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

    // JwtTokenSigner expects a String secret, so we pass the one from config
    public static TokenSigner createTokenSigner() {
        return new JwtTokenSigner(DEFAULT_CONFIG.jwtSecret());
    }

    public static TokenSigner createTokenSigner(String secret) {
        return new JwtTokenSigner(secret);
    }

    public static AccountService createAccountService(
            AccountRepository repo,
            NotificationRegistry notifReg,
            PasswordHasher hasher,
            TokenSigner signer,
            ServerConfig config
    ) {
        return new AccountServiceImpl(repo, hasher, signer, notifReg, config);
    }

    /**
     * Creates a fully wired AccountService with all default dependencies.
     */
    public static AccountService createDefaultAccountService() {
        return createAccountService(
                createAccountRepository(),
                createNotificationRegistry(),
                createPasswordHasher(),
                createTokenSigner(),
                DEFAULT_CONFIG
        );
    }
}