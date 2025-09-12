package com.yourco.ddz.engine.util;

import java.util.UUID;

public final class IdGenerator {
    private IdGenerator() {}
    public static String newGameId() { return "g-" + UUID.randomUUID(); }
}
