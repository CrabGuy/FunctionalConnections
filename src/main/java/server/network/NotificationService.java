package server.network;
import shared.dto.GameInfoData;
import java.util.Map;
public interface NotificationService {
    void notifyGameEnd(Map<String, GameInfoData> resultsByUsername);
}
