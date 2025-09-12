package com.yourco.ddz.engine.core;

import java.util.UUID;

public record SystemAction(String type, Object payload) implements GameAction {
    @Override public UUID playerId() { return null; }
}
