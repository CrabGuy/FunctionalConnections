package client.command;

import java.io.IOException;

public interface CommandLineInterface {
    void start() throws IOException;
    void processCommand(String command) throws IOException;
}
