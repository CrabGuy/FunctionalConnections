package server.network;

import java.util.Map;
import shared.dto.GameInfoData;

public interface NotificationService {
  void notifyGameEnd(Map<String, GameInfoData> resultsByUsername);
}
