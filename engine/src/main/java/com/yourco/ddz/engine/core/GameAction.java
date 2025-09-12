package com.yourco.ddz.engine.core;
import java.util.UUID;
public sealed interface GameAction permits PlayerAction, SystemAction { UUID playerId(); }