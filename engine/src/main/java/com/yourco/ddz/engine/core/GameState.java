package com.yourco.ddz.engine.core;

import com.yourco.ddz.engine.cards.Card;
import java.time.Instant;
import java.util.*;

public final class GameState {
  public PlayedHand getCurrentLead() {
    //    ArrayList<Card> cards = new ArrayList<Card>();
    //    cards.add(new Card(Card.Suit.CLUBS, Card.Rank.THREE));
    //    return new PlayedHand(ComboType.SINGLE, cards);
    return this.currentLead;
  }

  public List<Card> handOf(UUID uuid) {
    return this.hands.get(uuid);
  }

  public void setLandlordId(UUID landlord) {
    this.landlordId = landlord;
  }

  public void setCurrentLead(PlayedHand p) {
    if (p == null) {
      return;
    }
    Collections.sort(p.cards(), Comparator.comparing(Card::rank));
    this.currentLead = p;
  }

  public void setCurrentLeadPlayer(UUID uuid) {
    this.currentLeadPlayer = uuid;
  }

  public void setPassesInRow(int i) {
    this.pass_count = i;
  }

  //  public void setBottom(List<Card> cards) {}

  public Collection<Card> bottom() {
    return new ArrayList<Card>();
  }

  public int passesInRow() {
    return this.pass_count;
  }

  public UUID getCurrentLeadPlayer() {
    return this.currentLeadPlayer;
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

  private final Map<java.util.UUID, List<Card>> hands;

  private final Deque<GameAction> actionLog;

  private Phase phase;

  private int currentPlayerIndex;
  private Instant updatedAt;
  private int pass_count;
  private java.util.UUID landlordId;
  private PlayedHand currentLead;
  private java.util.UUID currentLeadPlayer;

  public GameState(String gameId, List<java.util.UUID> players) {
    this.gameId = Objects.requireNonNull(gameId);
    this.players = new ArrayList<>(players);
    this.hands = new HashMap<>();
    this.actionLog = new ArrayDeque<>();
    this.phase = Phase.LOBBY;
    this.currentPlayerIndex = 0;
    this.updatedAt = Instant.now();
    for (UUID p : this.players) {
      hands.put(p, new ArrayList<>());
    }
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
