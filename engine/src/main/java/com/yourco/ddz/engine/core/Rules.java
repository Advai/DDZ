package com.yourco.ddz.engine.core;

/** Game rules contract (hexagonal port). */
public interface Rules {
    /** Validate and mutate state for a given action; throws if invalid. */
    void apply(GameState state, GameAction action);

    /** True when the hand/game has reached a terminal state. */
    boolean isTerminal(GameState state);

    /** Called once when game reaches terminal state to compute scores, winners, etc. */
    void score(GameState state);
}
