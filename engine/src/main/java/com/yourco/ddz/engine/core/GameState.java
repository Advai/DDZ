package com.yourco.ddz.engine.core;

import java.time.Instant;
import java.util.*;

public final class GameState {
    public enum Phase { LOBBY, DEAL, BIDDING, PLAY, SCORING, TERMINATED }

    private final String gameId;
    private final List<UUID> players;
    private final Map<UUID, Object> hands; // replace Object with your Hand type later
    private final Deque<GameAction> actionLog;
    private Phase phase;
    private int currentPlayerIndex;
    private Instant updatedAt;

    public GameState(String gameId, List<UUID> players) {
        this.gameId = Objects.requireNonNull(gameId);
        this.players = new ArrayList<>(players);
        this.hands = new HashMap<>();
        this.actionLog = new ArrayDeque<>();
        this.phase = Phase.LOBBY;
        this.currentPlayerIndex = 0;
        this.updatedAt = Instant.now();
    }

    public String gameId() { return gameId; }
    public List<UUID> players() { return Collections.unmodifiableList(players); }
    public Map<UUID, Object> hands() { return Collections.unmodifiableMap(hands); }
    public Phase phase() { return phase; }
    public int currentPlayerIndex() { return currentPlayerIndex; }
    public UUID currentPlayerId() { return players.get(currentPlayerIndex); }
    public Deque<GameAction> actionLog() { return actionLog; }
    public Instant updatedAt() { return updatedAt; }

    // Mutators for engine use
    public void setPhase(Phase phase) { this.phase = phase; touch(); }
    public void setCurrentPlayerIndex(int idx) { this.currentPlayerIndex = idx; touch(); }
    public void nextPlayer() { this.currentPlayerIndex = (currentPlayerIndex + 1) % players.size(); touch(); }
    public void addAction(GameAction a) { this.actionLog.addLast(a); touch(); }
    private void touch() { this.updatedAt = Instant.now(); }
}
