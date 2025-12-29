package com.yourco.ddz.engine.core;

import java.util.*;

public final class GameLoop {
  private Rules rules;
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

  public Rules rules() {
    return rules;
  }

  public void updateRules(Rules newRules) {
    if (state.phase() != GameState.Phase.LOBBY && state.phase() != GameState.Phase.TERMINATED) {
      throw new IllegalStateException("Cannot update rules after game has started");
    }
    this.rules = Objects.requireNonNull(newRules);
  }

  public void resetForNewRound() {
    scored = false;
  }

  public void submit(GameAction a) {
    inbox.add(a);
  }

  public void tick() {
    while (!inbox.isEmpty()) {
      // If terminal and next action isn't a restart (SystemAction), stop processing
      if (rules.isTerminal(state)) {
        var next = inbox.peek();
        if (next == null || !(next instanceof SystemAction sa) || !"START".equals(sa.type())) {
          break; // Don't process actions in terminal state unless it's a START action
        }
      }

      var a = inbox.poll();
      try {
        rules.apply(state, a);
        state.addAction(a);
      } catch (IllegalStateException | IllegalArgumentException e) {
        // Invalid action - do not add to history, do not modify state
        // Re-throw so caller can handle (e.g., send error to client)
        throw e;
      }
    }
    if (rules.isTerminal(state) && !scored) {
      rules.score(state);
      scored = true;
    }
  }
}
