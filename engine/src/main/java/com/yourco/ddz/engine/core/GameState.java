package com.yourco.ddz.engine.core;

import com.yourco.ddz.engine.cards.Card;
import java.time.Instant;
import java.util.*;

public final class GameState {
  public PlayedHand getCurrentLead() {
    ArrayList<Card> cards = new ArrayList<Card>();
    cards.add(new Card(Card.Suit.CLUBS, Card.Rank.THREE));
    return new PlayedHand(ComboType.SINGLE, cards, 1, 1);
  }

  public List<Card> handOf(UUID uuid) {
    return new ArrayList<>();
  }

  public void setLandlordId(UUID landlord) {
    this.landlordId = landlord;
  }

  public UUID getLandlordId() {
    return this.landlordId;
  }

  public void setCurrentLead(Object o) {}

  public void setCurrentLeadPlayer(Object o) {}

  public void setPassesInRow(int i) {}

  public void setBottom(List<Card> cards) {}

  public Collection<Card> bottom() {
    return new ArrayList<Card>();
  }

  public int passesInRow() {
    return 0;
  }

  public Object getCurrentLeadPlayer() {
    return new Object();
  }

  public enum Phase {
    LOBBY,
    DEAL,
    BIDDING,
    PLAY,
    SCORING,
    TERMINATED
  }

  private final String gameId;

  private final List<java.util.UUID> players;

  private final Map<java.util.UUID, Object> hands;

  private final Deque<GameAction> actionLog;

  private Phase phase;

  private UUID landlordId;

  private int currentPlayerIndex;
  private Instant updatedAt;

  public GameState(String gameId, List<java.util.UUID> players) {
    this.gameId = Objects.requireNonNull(gameId);
    this.players = new ArrayList<>(players);
    this.hands = new HashMap<>();
    this.actionLog = new ArrayDeque<>();
    this.phase = Phase.LOBBY;
    this.currentPlayerIndex = 0;
    this.updatedAt = Instant.now();
  }

  public String gameId() {
    return gameId;
  }

  public List<java.util.UUID> players() {
    return Collections.unmodifiableList(players);
  }

  public Map<java.util.UUID, Object> hands() {
    return Collections.unmodifiableMap(hands);
  }

  public Phase phase() {
    return phase;
  }

  public int currentPlayerIndex() {
    return currentPlayerIndex;
  }

  public java.util.UUID currentPlayerId() {
    return players.get(currentPlayerIndex);
  }

  public Deque<GameAction> actionLog() {
    return actionLog;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public void setPhase(Phase p) {
    this.phase = p;
    touch();
  }

  public void setCurrentPlayerIndex(int i) {
    this.currentPlayerIndex = i;
    touch();
  }

  public void nextPlayer() {
    this.currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    touch();
  }

  public void addAction(GameAction a) {
    this.actionLog.addLast(a);
    touch();
  }

  private void touch() {
    this.updatedAt = Instant.now();
  }
}
