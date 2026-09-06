package server.network;

import server.account.AccountService;
import server.account.exceptions.AccountException;
import server.game.GameClock;
import server.game.ProposalService;
import server.game.exceptions.GameException;
import server.stats.LeaderboardService;
import server.stats.StatsService;
import shared.dto.*;

import java.net.InetSocketAddress;

public final class RequestDispatcherImpl implements RequestDispatcher {
    private final AccountService accountService;
    private final ProposalService proposalService;
    private final StatsService statsService;
    private final LeaderboardService leaderboardService;
    private final GameClock gameClock;

    public RequestDispatcherImpl(AccountService accountService,
                                 ProposalService proposalService,
                                 StatsService statsService,
                                 LeaderboardService leaderboardService,
                                 GameClock gameClock) {
        this.accountService = accountService;
        this.proposalService = proposalService;
        this.statsService = statsService;
        this.leaderboardService = leaderboardService;
        this.gameClock = gameClock;
    }

    @Override
    public ApiResponse<?> dispatch(ApiRequest request, InetSocketAddress remoteAddress) {
        try {
            return switch (request) {
                case RegisterRequest req -> success(accountService.register(req.username(), req.psw()));
                case LoginRequest req -> {
                    String token = accountService.login(
                            req.username(),
                            req.psw(),
                            req.udpPort(),
                            remoteAddress.getAddress().getHostAddress()
                    ).accountToken();
                    yield success(new LoginData(token));
                }
                case LogoutRequest req -> {
                    accountService.logout(req.accountToken());
                    yield success(new LogoutData());
                }
                case UpdateCredentialsRequest req -> success(accountService.updateCredentials(
                        req.oldUsername(),
                        req.newUsername(),
                        req.oldPsw(),
                        req.newPsw()
                ));
                case SubmitProposalRequest req -> {
                    long gameId = req.gameId() != null
                            ? req.gameId()
                            : gameClock.currentGameId(System.currentTimeMillis());
                    yield success(proposalService.submitProposal(
                            req.accountToken(),
                            gameId,
                            req.words()
                    ));
                }
                case RequestGameInfoRequest req -> success(proposalService.getGameInfo(
                        req.accountToken(),
                        req.gameId()
                ));
                case RequestGameStatsRequest req -> success(statsService.getGameStats(
                        req.accountToken(),
                        req.gameId()
                ));
                case RequestLeaderboardRequest req -> success(leaderboardService.getLeaderboard(
                        req.accountToken(),
                        req.playerName(),
                        req.topPlayers()
                ));
                case RequestPlayerStatsRequest req -> success(statsService.getPlayerStats(
                        req.accountToken()
                ));
                default -> failure(new ApiError(ErrorCode.INTERNAL_ERROR, "Unsupported operation"));
            };
        } catch (AccountException e) {
            return failure(new ApiError(e.errorCode(), e.getMessage()));
        } catch (GameException e) {
            return failure(new ApiError(e.errorCode(), e.getMessage()));
        } catch (Exception e) {
            return failure(new ApiError(ErrorCode.INTERNAL_ERROR, "Internal server error: " + e.getMessage()));
        }
    }

    private <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data);
    }

    private ApiResponse<?> failure(ApiError error) {
        return new ApiResponse<>(false, error, null);
    }
}
