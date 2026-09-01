package client.dto;

import shared.dto.GameInfoData;

/**
 * Client-side representation of the current game state.
 * Holds only the raw data received from the server via {@link GameInfoData}.
 * All derived values (score, mistakes, remaining words, player status) are computed
 * using pure functions from a separate utility class.
 *
 * @param gameInfo the raw game information as provided by the server.
 */
public record ClientGameState(GameInfoData gameInfo) {
}
