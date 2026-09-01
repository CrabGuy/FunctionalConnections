package client;

import java.util.Scanner;

public interface ClientState {
    void handle(Scanner scanner, ClientContext context);
}