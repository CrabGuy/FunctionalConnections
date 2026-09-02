package client;

import java.io.IOException;

/** Runs the interactive command-line client and processes individual commands. */
public interface CommandLineInterface {
    void start() throws IOException;

    void processCommand(String command) throws IOException;
}
