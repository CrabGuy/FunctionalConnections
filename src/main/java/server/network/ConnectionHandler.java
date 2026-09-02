package server.network;

import java.nio.channels.SocketChannel;

/**
 * NIO channel handler, one per accepted TCP connection, run via thread pool.
 * Reads/writes newline-delimited JSON, deserializes into the concrete ApiRequest subtype by 'operation'.
 */
public interface ConnectionHandler extends Runnable {
    
    /**
     * Attaches the socket channel to this handler for processing.
     * 
     * @param clientChannel the NIO channel of the accepted TCP connection.
     */
    void bind(SocketChannel clientChannel);
}