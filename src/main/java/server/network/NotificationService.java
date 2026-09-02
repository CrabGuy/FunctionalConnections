package server.network;

import shared.dto.GameStatsData;
import java.util.Collection;

/**
 * Service responsible for handling outgoing asynchronous communications.
 */
public interface NotificationService {

    /**
     * Sends game results to all participants.
     * Sends over UDP using NotificationRegistry lookups.
     * 
     * @param result the aggregated game statistics.
     * @param usernames the collection of participants to notify.
     */
    void notifyGameEnd(GameStatsData result, Collection<String> usernames);
}