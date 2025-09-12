package com.yourco.ddz.engine.core;

import java.util.UUID;

public record PlayerAction(UUID playerId, String type, Object payload) implements GameAction {}
