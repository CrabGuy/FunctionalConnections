package server.network;

import java.net.InetSocketAddress;
import server.account.AccountService;
import server.account.exceptions.AccountException;
import server.game.GameClock;
import server.game.ProposalService;
import server.game.exceptions.GameException;
import server.stats.LeaderboardService;
import server.stats.StatsService;
import server.stats.exceptions.PlayerNotFoundException;
import shared.dto.*;

/**
 * Implementation of {@link RequestDispatcher} that routes requests to the appropriate service
 * methods and handles exceptions.
 */
public record RequestDispatcherImpl(
    AccountService accountService,
    ProposalService proposalService,
    StatsService statsService,
    LeaderboardService leaderboardService,
    GameClock gameClock)
    implements RequestDispatcher {

  /** {@inheritDoc} */
  @Override
  public ApiResponse<?> dispatch(ApiRequest request, InetSocketAddress remoteAddress) {
    try {
      return switch (request) {
        case RegisterRequest req -> success(accountService.register(req.username(), req.psw()));
        case LoginRequest req -> {
          String token =
              accountService
                  .login(
                      req.username(),
                      req.psw(),
                      req.udpPort(),
                      remoteAddress.getAddress().getHostAddress())
                  .accountToken();
          proposalService.touchCurrentGame(token);
          yield success(new LoginData(token));
        }
        case LogoutRequest req -> {
          accountService.logout(req.accountToken());
          yield success(new LogoutData());
        }
        case UpdateCredentialsRequest req ->
            success(
                accountService.updateCredentials(
                    req.oldUsername(), req.newUsername(), req.oldPsw(), req.newPsw()));
        case SubmitProposalRequest req -> {
          long gameId =
              req.gameId() != null
                  ? req.gameId()
                  : gameClock.currentGameId(System.currentTimeMillis());
          yield success(proposalService.submitProposal(req.accountToken(), gameId, req.words()));
        }
        case RequestGameInfoRequest req ->
            success(proposalService.getGameInfo(req.accountToken(), req.gameId()));
        case RequestGameStatsRequest req ->
            success(statsService.getGameStats(req.accountToken(), req.gameId()));
        case RequestLeaderboardRequest req ->
            success(
                leaderboardService.getLeaderboard(
                    req.accountToken(), req.playerName(), req.topPlayers()));
        case RequestPlayerStatsRequest req ->
            success(statsService.getPlayerStats(req.accountToken()));
      };
    } catch (AccountException e) {
      return failure(new ApiError(e.errorCode(), e.getMessage()));
    } catch (GameException e) {
      return failure(new ApiError(e.errorCode(), e.getMessage()));
    } catch (PlayerNotFoundException e) {
      return failure(new ApiError(e.errorCode(), e.getMessage()));
    } catch (Exception e) {
      return failure(
          new ApiError(ErrorCode.INTERNAL_ERROR, "Internal server error: " + e.getMessage()));
    }
  }

  /**
   * Creates a successful API response.
   *
   * @param data the data to include
   * @param <T> the data type
   * @return the response
   */
  private <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(true, null, data);
  }

  /**
   * Creates a failure API response.
   *
   * @param error the error details
   * @return the response
   */
  private ApiResponse<?> failure(ApiError error) {
    return new ApiResponse<>(false, error, null);
  }
}
