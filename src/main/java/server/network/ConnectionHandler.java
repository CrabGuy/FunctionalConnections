package server.network;
import java.nio.channels.SocketChannel;
public interface ConnectionHandler extends Runnable {
    void bind(SocketChannel clientChannel);
}
