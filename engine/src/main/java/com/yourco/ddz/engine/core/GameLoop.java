package com.yourco.ddz.engine.core;

import java.util.*;

public final class GameLoop {
  private final Rules rules;
  private final GameState state;
  private final Queue<GameAction> inbox = new ArrayDeque<>();
  private boolean scored = false;

  public GameLoop(Rules rules, GameState initialState) {
    this.rules = Objects.requireNonNull(rules);
    this.state = Objects.requireNonNull(initialState);
  }

  public GameState state() {
    return state;
  }

  public void submit(GameAction a) {
    inbox.add(a);
  }

  public void tick() {
    // Special case: allow restarting from terminal state
    if (rules.isTerminal(state) && !inbox.isEmpty()) {
      var nextAction = inbox.peek();
      if (nextAction instanceof SystemAction sa && "START".equals(sa.type())) {
        // Process the restart action
        inbox.poll();
        rules.apply(state, sa);
        state.addAction(sa);
        // Reset scored flag for new game
        scored = false;
      }
    }

    // Process actions while game is not terminal
    while (!inbox.isEmpty() && !rules.isTerminal(state)) {
      var a = inbox.poll();
      rules.apply(state, a);
      state.addAction(a);
    }

    // Score the game when it reaches terminal state
    if (rules.isTerminal(state) && !scored) {
      rules.score(state);
      scored = true;
    }
  }
}
