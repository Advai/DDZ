package com.yourco.ddz.engine.core;

public interface Rules {
  void apply(GameState state, GameAction action);

  boolean isTerminal(GameState state);

  void score(GameState state);
}
