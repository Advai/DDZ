package com.yourco.ddz.engine.core;

import com.yourco.ddz.engine.cards.Card;
import com.yourco.ddz.engine.cards.Deck;
import java.util.*;

public final class DdzRules implements Rules {
  private final PlayDetector detector;
  private final PlayComparator comparator;
  private final GameConfig config;

  public DdzRules(GameConfig config) {
    this.detector = HandDetector.defaultDdz();
    this.comparator = Objects.requireNonNull(new SimplePlayComparator());
    this.config = Objects.requireNonNull(config);
  }

  /** Convenience factory for standard 3-player game. */
  public static DdzRules standard3Player() {
    return new DdzRules(GameConfig.standard(3));
  }

  /** Convenience factory for a standard game with specified player count. */
  public static DdzRules standard(int playerCount) {
    return new DdzRules(GameConfig.standard(playerCount));
  }

  public GameConfig getConfig() {
    return config;
  }

  @Override
  public void apply(GameState s, GameAction a) {
    switch (s.phase()) {
      case LOBBY -> onStart(s, a); // expect SystemAction("START")
      case PLAY -> onPlay(s, a); // expect PlayerAction("PLAY", List<Card> or null for PASS)
      case BIDDING -> onBidOrSelectLandlord(s, a);
      case SCORING -> throw new IllegalStateException("Game over");
      case TERMINATED -> onRestart(s, a); // Allow restarting from terminated state
      default -> throw new IllegalStateException("Unsupported phase: " + s.phase());
    }
  }

  private void onBidOrSelectLandlord(GameState s, GameAction a) {
    if (!(a instanceof PlayerAction pa)) {
      throw new IllegalArgumentException("Expected PlayerAction");
    }

    // Check if we're in landlord selection mode
    if (s.getAwaitingLandlordSelection() != null) {
      DdzRulesLandlordSelection.onSelectLandlord(s, pa, config);
    } else {
      DdzRulesBidding.onBid(s, a, config);
    }
  }

  @Override
  public boolean isTerminal(GameState s) {
    return s.phase() == GameState.Phase.TERMINATED;
  }

  @Override
  public void score(GameState s) {
    if (s.phase() != GameState.Phase.TERMINATED) {
      throw new IllegalStateException("Cannot score a game that hasn't ended");
    }

    // Find the winner (player with no cards)
    UUID winner = null;
    for (UUID p : s.players()) {
      if (s.handOf(p).isEmpty()) {
        winner = p;
        break;
      }
    }

    if (winner == null) {
      System.out.println("No winner found - game may have ended abnormally");
      return;
    }

    boolean landlordWon = s.isLandlord(winner);

    // Calculate base score from bid
    int baseScore = s.getHighestBid();
    if (baseScore == 0) baseScore = 1; // Default if no one bid

    // Calculate multipliers
    int multiplierExponent = 0;

    // Bombs and rockets each double the score
    multiplierExponent += s.getBombsPlayed();
    multiplierExponent += s.getRocketsPlayed();

    // Spring/Anti-spring detection
    boolean spring = false;
    boolean antiSpring = false;

    if (landlordWon && !s.getFarmersPlayed()) {
      spring = true;
      multiplierExponent++; // Spring doubles the score
      System.out.println("SPRING! Landlord won before farmers played any cards!");
    } else if (!landlordWon && !s.getLandlordPlayed()) {
      antiSpring = true;
      multiplierExponent++; // Anti-spring doubles the score
      System.out.println("ANTI-SPRING! Farmers won before landlord played any cards!");
    }

    // Final score = base × 2^multiplier
    int finalScore = baseScore * (int) Math.pow(2, multiplierExponent);

    System.out.println("=== SCORING ===");
    System.out.println("Winner: " + winner);
    System.out.println("Base score (bid): " + baseScore);
    System.out.println("Bombs played: " + s.getBombsPlayed());
    System.out.println("Rockets played: " + s.getRocketsPlayed());
    System.out.println("Spring: " + spring);
    System.out.println("Anti-spring: " + antiSpring);
    System.out.println("Multiplier exponent: " + multiplierExponent);
    System.out.println("Final score: " + finalScore);

    // Distribute scores
    int numFarmers = s.players().size() - config.getLandlordCount();

    if (landlordWon) {
      // Landlords win: each landlord gains finalScore from each farmer
      for (UUID landlord : s.getLandlordIds()) {
        s.addScore(landlord, finalScore * numFarmers);
      }
      for (UUID p : s.players()) {
        if (!s.isLandlord(p)) {
          s.addScore(p, -finalScore * config.getLandlordCount());
        }
      }
      System.out.println("Each landlord +" + (finalScore * numFarmers));
      System.out.println("Each farmer -" + (finalScore * config.getLandlordCount()));
    } else {
      // Farmers win: each landlord loses finalScore to each farmer
      for (UUID landlord : s.getLandlordIds()) {
        s.addScore(landlord, -finalScore * numFarmers);
      }
      for (UUID p : s.players()) {
        if (!s.isLandlord(p)) {
          s.addScore(p, finalScore * config.getLandlordCount());
        }
      }
      System.out.println("Each landlord -" + (finalScore * numFarmers));
      System.out.println("Each farmer +" + (finalScore * config.getLandlordCount()));
    }

    System.out.println("Final scores: " + s.getScores());
    System.out.println("===============");
  }

  /* ====== RESTART → reset state and start a new game ====== */
  private void onRestart(GameState s, GameAction a) {
    if (!(a instanceof SystemAction sa) || !"START".equals(sa.type())) {
      throw new IllegalArgumentException("Expected System START");
    }

    System.out.println("===============");
    System.out.println("RESTARTING GAME: " + s.gameId());
    System.out.println("===============");

    // Reset all game state while keeping players
    s.resetForNewGame();

    // Now start a new game (deal cards, enter BIDDING)
    onStart(s, a);
  }

  /* ====== START → deal cards, save bottom cards, enter BIDDING ====== */
  private void onStart(GameState s, GameAction a) {
    if (!(a instanceof SystemAction sa) || !"START".equals(sa.type())) {
      throw new IllegalArgumentException("Expected System START");
    }

    var pool = getCardPool();

    deal_player_hands(s, pool);

    // Save remaining cards as "bottom" for landlord
    s.setBottom(List.copyOf(pool));
    System.out.println("Bottom cards (for landlord): " + pool);

    // Sort all hands for display
    for (UUID p : s.players()) {
      Collections.sort(s.handOf(p), Comparator.comparing(Card::rank));
      System.out.println("Sorted hand for " + p + ": " + s.handOf(p));
    }

    // Initialize bidding state
    s.clearBiddingState();
    s.setCurrentPlayerIndex(0);

    // Transition to BIDDING phase
    s.setPhase(GameState.Phase.BIDDING);
  }

  private void deal_player_hands(GameState s, ArrayList<Card> pool) {
    int num_cards_per_player = config.getCardsPerPlayer();
    for (int i = 0; i < num_cards_per_player; i++) {
      for (UUID p : s.players()) {
        s.handOf(p).add(pool.removeFirst());
        System.out.println(s.handOf(p));
      }
    }
  }

  private ArrayList<Card> getCardPool() {
    var pool = new ArrayList<Card>();
    for (int i = 0; i < config.getDeckCount(); i++) {
      var deck = new Deck();
      pool.addAll(deck.asList());
    }
    Collections.shuffle(pool);
    return pool;
  }

  /* ====== PLAY → detect/validate/apply or PASS ====== */
  private void onPlay(GameState s, GameAction a) {
    if (!(a instanceof PlayerAction pa)) {
      throw new IllegalArgumentException("Expected PlayerAction");
    }
    if (!pa.playerId().equals(s.currentPlayerId())) {
      throw new IllegalStateException("Not your turn");
    }

    // PASS: null payload (leader cannot pass if no current lead)
    if (pa.payload() == null) {
      onPass(s);
      return;
    }

    // Expect List<Card> as the raw move
    if (!(pa.payload() instanceof List<?> raw) || raw.isEmpty()) {
      throw new IllegalArgumentException("Bad payload");
    }
    @SuppressWarnings("unchecked")
    List<Card> cards = (List<Card>) raw;

    // Detect combo
    var maybe = detector.detect(cards);
    if (maybe.isEmpty()) throw new IllegalStateException("Invalid combo");
    var hand = maybe.get();

    // Validate kicker rules for 5+ player games
    if (config.getPlayerCount() >= 5) {
      if (hand.type() == ComboType.TRIPLE_WITH_SINGLE) {
        throw new IllegalStateException(
            "Single kickers not allowed in 5+ player games. Use TRIPLE_WITH_PAIR instead.");
      }
      if (hand.type() == ComboType.AIRPLANE_WITH_SINGLES) {
        throw new IllegalStateException(
            "Single kickers not allowed in 5+ player games. Use AIRPLANE_WITH_PAIRS instead.");
      }
      if (hand.type() == ComboType.BOMB_WITH_SINGLES) {
        throw new IllegalStateException(
            "Single kickers not allowed in 5+ player games. Use BOMB_WITH_PAIRS instead.");
      }
    }

    // Own the cards
    System.out.println("hand of pa " + pa.playerId() + ": " + s.handOf(pa.playerId()));
    System.out.println("Hand Type: " + hand.type());
    System.out.println("Cards in Hand: " + hand.cards());
    if (!s.handOf(pa.playerId()).containsAll(hand.cards())) {
      throw new IllegalStateException("Card(s) not in hand");
    }

    // Must beat current lead if exists
    var lead = s.getCurrentLead();
    if (lead != null) {
      int cmp = comparator.compare(hand, lead);
      if (cmp <= 0) throw new IllegalStateException("Does not beat current lead");
    }

    // Apply play - remove each card individually to avoid removing all duplicates
    List<Card> playerHand = s.handOf(pa.playerId());
    for (Card card : hand.cards()) {
      playerHand.remove(card); // Remove only first occurrence
    }
    s.setCurrentLead(hand);
    s.setCurrentLeadPlayer(pa.playerId());
    s.setPassesInRow(0);

    // Track bombs and rockets for scoring
    // Note: Only PURE bombs count for multiplier, not bombs with kickers
    if (hand.type() == ComboType.BOMB) {
      s.incrementBombsPlayed();
      System.out.println("PURE BOMB played! Total bombs: " + s.getBombsPlayed());
    } else if (hand.type() == ComboType.ROCKET) {
      s.incrementRocketsPlayed();
      System.out.println("ROCKET played! Total rockets: " + s.getRocketsPlayed());
    } else if (hand.type() == ComboType.BOMB_WITH_SINGLES
        || hand.type() == ComboType.BOMB_WITH_PAIRS) {
      System.out.println("BOMB WITH KICKERS played (does not count for multiplier)");
    }

    // Track first play for spring/anti-spring detection
    if (s.isLandlord(pa.playerId())) {
      s.setLandlordPlayed(true);
    } else {
      s.setFarmersPlayed(true);
    }

    // Terminal: hand empty
    // Win condition: ANY landlord empties hand = landlords win, ANY farmer empties = farmers win
    if (s.handOf(pa.playerId()).size() == 0) {
      System.out.println("Player " + pa.playerId() + " won!");
      boolean landlordWon = s.isLandlord(pa.playerId());
      System.out.println(landlordWon ? "LANDLORDS WIN!" : "FARMERS WIN!");
      s.setPhase(GameState.Phase.TERMINATED);
      score(s); // Calculate scores when game ends
      return;
    }

    // Next turn
    s.nextPlayer();
  }

  private void onPass(GameState s) {
    if (s.getCurrentLead() == null) {
      throw new IllegalStateException("Leader must play; cannot PASS");
    }
    s.setPassesInRow(s.passesInRow() + 1);
    s.nextPlayer();

    // Round closes when (totalPlayers - 1) consecutive passes occur
    // This means everyone except the current lead player has passed
    int passesNeeded = s.players().size() - 1;

    if (s.passesInRow() >= passesNeeded) {
      s.setCurrentLead(null);
      s.setCurrentLeadPlayer(null);
      s.setPassesInRow(0);
      System.out.println(
          "Round over after "
              + passesNeeded
              + " passes. Next player leads: "
              + s.players().get(s.currentPlayerIndex()));
    }
  }
}
