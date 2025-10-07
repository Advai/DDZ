package com.yourco.ddz.engine.demo;

import com.yourco.ddz.engine.core.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class DemoRules implements Rules {
  private final AtomicInteger applied = new AtomicInteger();

  @Override
  public void apply(GameState s, GameAction a) {
    s.nextPlayer();
    applied.incrementAndGet();
  }

  @Override
  public boolean isTerminal(GameState s) {
    return applied.get() >= 3;
  }

  @Override
  public void score(GameState s) {}
}
