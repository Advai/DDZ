package com.yourco.ddz.engine.core;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;

/** Single-threaded headless game loop. Feed actions; call tick() to advance. */
public final class GameLoop {
    private final Rules rules;
    private final GameState state;
    private final Queue<GameAction> inbox = new ArrayDeque<>();
    private boolean scored = false;

    public GameLoop(Rules rules, GameState initialState) {
        this.rules = Objects.requireNonNull(rules);
        this.state = Objects.requireNonNull(initialState);
    }

    public GameState state() { return state; }

    /** Enqueue an action to be applied on the next tick. */
    public void submit(GameAction action) {
        inbox.add(action);
    }

    /** Apply all queued actions atomically for this frame. */
    public void tick() {
        while (!inbox.isEmpty() && !rules.isTerminal(state)) {
            var action = inbox.poll();
            rules.apply(state, action);
            state.addAction(action);
        }
        if (rules.isTerminal(state) && !scored) {
            rules.score(state);
            scored = true;
        }
    }
}
