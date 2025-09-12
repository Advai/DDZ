package com.yourco.ddz.engine.core;

import java.util.UUID;

/** Marker interface for player or system actions */
public sealed interface GameAction permits PlayerAction, SystemAction {
    UUID playerId(); // may be null for SystemAction
}
