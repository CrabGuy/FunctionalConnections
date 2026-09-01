package client;

import shared.DataContracts;

public interface GameBoardRenderer {
    void render(DataContracts.GameStateDto board);
}