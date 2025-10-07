package com.yourco.ddz.server.core;

import com.yourco.ddz.engine.core.GameLoop;
import com.yourco.ddz.engine.core.GameState;
import com.yourco.ddz.engine.demo.DemoRules;
import java.util.List;
import java.util.UUID;

public record GameInstance(String gameId, GameLoop loop) {
  public static GameInstance newDemo(String id) {
    var players = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    var state = new GameState(id, players);
    var loop = new GameLoop(new DemoRules(), state);
    return new GameInstance(id, loop);
  }
}
