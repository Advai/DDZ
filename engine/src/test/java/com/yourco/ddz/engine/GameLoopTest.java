package com.yourco.ddz.engine;

import com.yourco.ddz.engine.core.*;
import com.yourco.ddz.engine.demo.DemoRules;
import com.yourco.ddz.engine.util.IdGenerator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class GameLoopTest {
    @Test
    void tick_applies_actions_and_terminates_demo() {
        var players = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        var state = new GameState(IdGenerator.newGameId(), players);
        var loop = new GameLoop(new DemoRules(), state);

        loop.submit(new PlayerAction(players.get(0), "PLAY", "x"));
        loop.submit(new PlayerAction(players.get(1), "PLAY", "y"));
        loop.submit(new PlayerAction(players.get(2), "PLAY", "z"));

        loop.tick();

        assertEquals(3, state.actionLog().size(), "should apply all actions");
        assertNotNull(state.currentPlayerId());
    }
}
