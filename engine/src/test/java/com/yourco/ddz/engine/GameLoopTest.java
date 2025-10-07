package com.yourco.ddz.engine;

import static org.junit.jupiter.api.Assertions.*;

import com.yourco.ddz.engine.core.*;

// public class GameLoopTest {
//  @Test
//  void tick_applies_actions_and_terminates_demo() {
//    var players = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
//    var state = new GameState("g-1", players);
//    var loop = new GameLoop(new DemoRules(), state);
//    loop.submit(new PlayerAction(players.get(0), "PLAY", "x"));
//    loop.submit(new PlayerAction(players.get(1), "PLAY", "y"));
//    loop.submit(new PlayerAction(players.get(2), "PLAY", "z"));
//    loop.tick();
//    assertEquals(3, state.actionLog().size());
//    assertNotNull(state.currentPlayerId());
//  }
// }
