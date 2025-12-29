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
    // Also set as the first landlord in the list for multi-landlord support
    if (!landlordIds.contains(landlord)) {
      landlordIds.clear();
      landlordIds.add(landlord);
    }
  }

  public UUID getLandlordId() {
    return this.landlordId;
  }

  public void setLandlordIds(List<UUID> landlords) {
    this.landlordIds = new ArrayList<>(landlords);
    // Set first landlord as primary for backwards compatibility
    if (!landlords.isEmpty()) {
      this.landlordId = landlords.get(0);
    }
  }

  public List<UUID> getLandlordIds() {
    return Collections.unmodifiableList(landlordIds);
  }

  public boolean isLandlord(UUID playerId) {
    return landlordIds.contains(playerId);
  }

  public void setCurrentLead(PlayedHand p) {
    if (p == null) {
      this.currentLead = null;
      return;
    }
    this.currentLead = p;
  }

  public void setCurrentLeadPlayer(UUID uuid) {
    this.currentLeadPlayer = uuid;
  }

  public void setPassesInRow(int i) {
    this.pass_count = i;
  }

  public void setBottom(List<Card> cards) {
    this.bottom = new ArrayList<>(cards);
  }

  public List<Card> bottom() {
    return bottom == null ? new ArrayList<>() : Collections.unmodifiableList(bottom);
  }

  public void setLandlordBottomCards(UUID landlordId, List<Card> cards) {
    landlordBottomCards.put(landlordId, new ArrayList<>(cards));
  }

  public List<Card> getLandlordBottomCards(UUID landlordId) {
    return landlordBottomCards.getOrDefault(landlordId, new ArrayList<>());
  }

  public void setPlayerBid(UUID playerId, int bid) {
    playerBids.put(playerId, bid);
  }

  public int getPlayerBid(UUID playerId) {
    return playerBids.getOrDefault(playerId, 0);
  }

  public Map<UUID, Integer> getAllBids() {
    return Collections.unmodifiableMap(playerBids);
  }

  public boolean hasEveryoneBid() {
    return playerBids.size() == players.size();
  }

  public int getHighestBid() {
    return playerBids.values().stream().max(Integer::compare).orElse(0);
  }

  public List<UUID> getHighestBidders() {
    int maxBid = getHighestBid();
    if (maxBid == 0) return List.of();

    return playerBids.entrySet().stream()
        .filter(e -> e.getValue() == maxBid)
        .map(Map.Entry::getKey)
        .toList();
  }

  public void clearBiddingState() {
    playerBids.clear();
    biddingRoundCount = 0;
  }

  public int passesInRow() {
    return this.pass_count;
  }

  public void incrementBombsPlayed() {
    this.bombsPlayed++;
  }

  public void incrementRocketsPlayed() {
    this.rocketsPlayed++;
  }

  public int getBombsPlayed() {
    return bombsPlayed;
  }

  public int getRocketsPlayed() {
    return rocketsPlayed;
  }

  public void setLandlordPlayed(boolean played) {
    this.landlordPlayed = played;
  }

  public void setFarmersPlayed(boolean played) {
    this.farmersPlayed = played;
  }

  public boolean getLandlordPlayed() {
    return landlordPlayed;
  }

  public boolean getFarmersPlayed() {
    return farmersPlayed;
  }

  public Map<UUID, Integer> getScores() {
    return Collections.unmodifiableMap(scores);
  }

  public void setScore(UUID playerId, int score) {
    this.scores.put(playerId, score);
  }

  public void addScore(UUID playerId, int delta) {
    this.scores.merge(playerId, delta, Integer::sum);
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

  // Player metadata
  private final Map<UUID, String> playerNames = new HashMap<>();
  private final Map<UUID, Boolean> playerConnected = new HashMap<>();

  private Phase phase;

  private UUID landlordId; // Primary landlord (for backwards compatibility)
  private List<UUID> landlordIds = new ArrayList<>(); // All landlords (multi-landlord support)

  private int currentPlayerIndex;
  private Instant updatedAt;
  private int pass_count;
  private PlayedHand currentLead;
  private java.util.UUID currentLeadPlayer;

  // Bidding state
  private Map<UUID, Integer> playerBids = new HashMap<>(); // Track each player's bid
  private int biddingRoundCount = 0;
  private List<Card> bottom = new ArrayList<>();
  private Map<UUID, List<Card>> landlordBottomCards =
      new HashMap<>(); // Track which cards are bottom cards for each landlord

  // Landlord selection state (for interactive snake draft)
  private UUID awaitingLandlordSelection = null; // Who needs to pick next landlord
  private List<UUID> selectedLandlords = new ArrayList<>(); // Landlords selected so far

  // Scoring state
  private Map<UUID, Integer> scores = new HashMap<>();
  private int bombsPlayed = 0;
  private int rocketsPlayed = 0;
  private boolean landlordPlayed = false;
  private boolean farmersPlayed = false;

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
      playerConnected.put(p, true); // All players start as connected
    }
  }

  public void setPlayerName(UUID playerId, String name) {
    playerNames.put(playerId, name);
  }

  public String getPlayerName(UUID playerId) {
    return playerNames.getOrDefault(playerId, "Player");
  }

  public Map<UUID, String> getPlayerNames() {
    return Collections.unmodifiableMap(playerNames);
  }

  public void setPlayerConnected(UUID playerId, boolean connected) {
    playerConnected.put(playerId, connected);
  }

  public boolean isPlayerConnected(UUID playerId) {
    return playerConnected.getOrDefault(playerId, false);
  }

  public Map<UUID, Boolean> getPlayerConnectionStatus() {
    return Collections.unmodifiableMap(playerConnected);
  }

  /**
   * Reset the game state to start a new game with the same players. Clears all game-specific state
   * while preserving player list and metadata.
   */
  public void resetForNewGame() {
    // Clear hands
    for (UUID playerId : players) {
      hands.put(playerId, new ArrayList<>());
    }

    // Reset phase
    phase = Phase.LOBBY;

    // Reset landlord info
    landlordId = null;
    landlordIds.clear();

    // Reset current player
    currentPlayerIndex = 0;

    // Clear play state
    currentLead = null;
    currentLeadPlayer = null;
    pass_count = 0;

    // Clear bidding state
    playerBids.clear();
    biddingRoundCount = 0;
    bottom.clear();
    landlordBottomCards.clear();

    // Clear landlord selection state
    awaitingLandlordSelection = null;
    selectedLandlords.clear();

    // Clear scoring state (per-game scores)
    scores.clear();
    bombsPlayed = 0;
    rocketsPlayed = 0;
    landlordPlayed = false;
    farmersPlayed = false;

    // Update timestamp
    updatedAt = Instant.now();

    // Clear action log
    actionLog.clear();
  }

  public void addPlayer(UUID playerId, String name) {
    if (!players.contains(playerId)) {
      players.add(playerId);
      hands.put(playerId, new ArrayList<>());
      playerNames.put(playerId, name);
      playerConnected.put(playerId, true);
      touch();
    }
  }

  public void setAwaitingLandlordSelection(UUID playerId) {
    this.awaitingLandlordSelection = playerId;
  }

  public UUID getAwaitingLandlordSelection() {
    return awaitingLandlordSelection;
  }

  public void addSelectedLandlord(UUID playerId) {
    if (!selectedLandlords.contains(playerId)) {
      selectedLandlords.add(playerId);
    }
  }

  public List<UUID> getSelectedLandlords() {
    return Collections.unmodifiableList(selectedLandlords);
  }

  public void clearLandlordSelection() {
    awaitingLandlordSelection = null;
    selectedLandlords.clear();
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
    if (players.isEmpty()) {
      return null;
    }
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
