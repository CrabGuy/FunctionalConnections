package client.notification;

import java.io.IOException;
import java.util.function.Consumer;
import shared.dto.GameEndNotification;

public interface NotificationListener {
    void start(int udpPort, Consumer<GameEndNotification> onGameEnd) throws IOException;
    void stop();
}