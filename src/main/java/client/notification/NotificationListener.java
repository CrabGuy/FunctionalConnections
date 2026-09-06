package client.notification;

import shared.dto.GameInfoData;

import java.io.IOException;
import java.util.function.Consumer;

public interface NotificationListener {
    void start(int udpPort, Consumer<GameInfoData> onGameEnd) throws IOException;
    void stop();
}
