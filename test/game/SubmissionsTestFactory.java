package test.game;

import server.account.AccountService;
import server.account.exceptions.InvalidTokenException;
import server.dto.AccountPrincipal;
import server.game.*;
import shared.dto.LoginData;
import shared.dto.RegisterData;
import shared.dto.UpdateCredentialsData;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating test instances of the Proposal submission & per-player game state components (Slice C).
 * Uses the real implementation classes from server.game.
 */
public class SubmissionsTestFactory {

    // ---------------------- GameClock ----------------------

    public static GameClock createGameClock(long gameDurationMillis) {
        return new GameClockImpl(gameDurationMillis);
    }

    // ---------------------- GameRepository ----------------------

    public static GameRepository createGameRepository(String gameDataFilePath) throws IOException {
        return new FileGameRepository(gameDataFilePath);
    }

    // ---------------------- PlayerGameRepository ----------------------

    public static PlayerGameRepository createPlayerGameRepository() {
        return new InMemoryPlayerGameRepository();
    }

    // ---------------------- AccountService stub ----------------------

    public static AccountService createStubAccountService() {
        return new StubAccountService();
    }

    // ---------------------- ProposalService ----------------------

    public static ProposalService createProposalService(
            AccountService accountService,
            GameClock clock,
            GameRepository gameRepository,
            PlayerGameRepository playerGameRepository) {
        return new ProposalServiceImpl(accountService, gameRepository, clock, playerGameRepository);
    }

    // ------------------------------------------------------------------
    // Stub account service (not part of production)
    // ------------------------------------------------------------------
    private static class StubAccountService implements AccountService {
        private final Map<String, String> tokens = new HashMap<>();

        public StubAccountService() {
            tokens.put("token1", "alice");
            tokens.put("token2", "bob");
        }

        @Override
        public RegisterData register(String username, String password) {
            throw new UnsupportedOperationException("Not used in this test");
        }

        @Override
        public LoginData login(String username, String password, int udpPort, String remoteAddress) {
            throw new UnsupportedOperationException("Not used in this test");
        }

        @Override
        public void logout(String accountToken) {
            // no-op
        }

        @Override
        public UpdateCredentialsData updateCredentials(String oldUsername, String newUsername,
                                                        String oldPassword, String newPassword) {
            throw new UnsupportedOperationException("Not used in this test");
        }

        @Override
        public AccountPrincipal resolve(String accountToken) throws InvalidTokenException {
            String username = tokens.get(accountToken);
            if (username == null) {
                throw new InvalidTokenException("Unknown token");
            }
            return new AccountPrincipal(username, Long.MAX_VALUE);
        }
    }
}