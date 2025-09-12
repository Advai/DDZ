package com.yourco.ddz.engine.demo;

import com.yourco.ddz.engine.core.*;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minimal placeholder rules that demonstrate the loop:
 * - The game terminates after 3 valid actions.
 */
public final class DemoRules implements Rules {
    private final AtomicInteger applied = new AtomicInteger();

    @Override
    public void apply(GameState state, GameAction action) {
        // Accept any action and advance player turn.
        state.nextPlayer();
        applied.incrementAndGet();
    }

    @Override
    public boolean isTerminal(GameState state) {
        return applied.get() >= 3;
    }

    @Override
    public void score(GameState state) {
        // no-op for demo
    }
}
