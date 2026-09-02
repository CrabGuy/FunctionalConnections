package client;
import java.io.IOException;
import java.util.function.Consumer;
import shared.dto.GameInfoData;
public interface NotificationListener {
    void start(int udpPort, Consumer<GameInfoData> onGameEnd) throws IOException;
    void stop();
}
