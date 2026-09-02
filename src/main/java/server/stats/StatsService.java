package server.stats;

import shared.dto.GameStatsData;
import shared.dto.PlayerStatsData;
import server.account.exceptions.InvalidTokenException;
import server.game.exceptions.GameNotFoundException;

/**
 * Service responsible for calculating statistics for games and players.
 * The server should NOT save game information like how many players won or average score.
 * These values should be calculated from the PlayerGames saved data, which is just of guesses from players for a certain game, score and other values will be calculated from a function starting from that data.
 */
public interface StatsService {

    /**
     * Retrieves aggregated statistics for a specific game.
     * Aggregates over PlayerGameRepository.findByGame.
     *
     * @param gameId the ID of the game to calculate stats for. Option to specify current game.
     * @return GameStatsData containing metrics such as total participants, completed players, winners, and average score.
     * @throws GameNotFoundException if the specified game ID does not exist.
     */
    GameStatsData getGameStats(Long gameId) throws GameNotFoundException;

    /**
     * Retrieves lifetime statistics for a specific player.
     * Aggregates over PlayerGameRepository.findByUsername.
     *
     * @param accountToken the access token of the requesting player, formatted as a JWT.
     * @return PlayerStatsData containing puzzles completed, win/loss rate, streaks, and mistake histogram.
     * @throws InvalidTokenException if the account token is missing, invalid, or expired.
     */
    PlayerStatsData getPlayerStats(String accountToken) throws InvalidTokenException;
}