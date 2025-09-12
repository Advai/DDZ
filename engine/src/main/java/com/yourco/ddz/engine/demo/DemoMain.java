package com.yourco.ddz.engine.demo;

import com.yourco.ddz.engine.core.*;
import com.yourco.ddz.engine.util.IdGenerator;

import java.util.List;
import java.util.UUID;

public final class DemoMain {
    public static void main(String[] args) {
        var players = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        var state = new GameState(IdGenerator.newGameId(), players);
        var loop = new GameLoop(new DemoRules(), state);

        // Submit 3 dummy actions; after tick() the demo terminates.
        loop.submit(new PlayerAction(players.get(0), "PLAY", "demo"));
        loop.submit(new PlayerAction(players.get(1), "PLAY", "demo"));
        loop.submit(new PlayerAction(players.get(2), "PLAY", "demo"));

        loop.tick();

        System.out.println("Game " + state.gameId() + " phase=" + state.phase()
                + " actions=" + state.actionLog().size()
                + " currentPlayer=" + state.currentPlayerId());
    }
}
