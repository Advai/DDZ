package com.yourco.ddz.engine.demo;

import com.yourco.ddz.engine.cards.Card;
import com.yourco.ddz.engine.cards.Deck;
import com.yourco.ddz.engine.core.*;
import java.util.*;

public final class DdzRules3p implements Rules {
  private final PlayDetector detector;
  private final PlayComparator comparator;

  public DdzRules3p(PlayDetector detector, PlayComparator comparator) {
    this.detector = Objects.requireNonNull(detector);
    this.comparator = Objects.requireNonNull(comparator);
  }

  /** Convenience factory using stub detector/comparator so the project compiles now. */
  public static DdzRules3p withStubs() {
    return new DdzRules3p(new SimplePlayDetector(), new SimplePlayComparator());
  }

  @Override
  public void apply(GameState s, GameAction a) {
    switch (s.phase()) {
      case LOBBY -> onStart(s, a); // expect SystemAction("START")
      case PLAY -> onPlay(s, a); // expect PlayerAction("PLAY", List<Card> or null for PASS)
      case SCORING, TERMINATED -> throw new IllegalStateException("Game over");
      default -> throw new IllegalStateException("Unsupported phase: " + s.phase());
    }
  }

  @Override
  public boolean isTerminal(GameState s) {
    return s.phase() == GameState.Phase.TERMINATED;
  }

  @Override
  public void score(GameState s) {
    // TODO: settle scores (landlord vs peasants, multipliers, etc.)
  }

  /* ====== START → deal, choose landlord (player 0 for now), enter PLAY ====== */
  private void onStart(GameState s, GameAction a) {
    if (!(a instanceof SystemAction sa) || !"START".equals(sa.type())) {
      throw new IllegalArgumentException("Expected System START");
    }
    // deal 17/17/17 + bottom 3
    var deck = new Deck();
    var pool = new ArrayList<>(deck.asList());
    Collections.shuffle(pool);

    for (int i = 0; i < 17; i++) {
      for (UUID p : s.players()) {
        s.handOf(p).addAll(List.of(pool.remove(0)));
      }
    }
    s.setBottom(List.copyOf(pool)); // remaining 3

    // landlord = player 0 (placeholder; add bidding later)
    UUID landlord = s.players().get(0);
    s.setLandlordId(landlord);
    s.handOf(landlord).addAll(s.bottom());

    // landlord starts
    s.setCurrentPlayerIndex(0);
    s.setCurrentLead(null);
    s.setCurrentLeadPlayer(null);
    s.setPassesInRow(0);
    s.setPhase(GameState.Phase.PLAY);
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

    // Own the cards
    if (!s.handOf(pa.playerId()).containsAll(hand.cards())) {
      throw new IllegalStateException("Card(s) not in hand");
    }

    // Must beat current lead if exists
    var lead = s.getCurrentLead();
    if (lead != null) {
      int cmp = comparator.compare(hand, lead);
      if (cmp <= 0) throw new IllegalStateException("Does not beat current lead");
    }

    // Apply play
    s.handOf(pa.playerId()).removeAll(hand.cards());
    s.setCurrentLead(hand);
    s.setCurrentLeadPlayer(pa.playerId());
    s.setPassesInRow(0);

    // Terminal: hand empty
    if (s.handOf(pa.playerId()).size() == 0) {
      s.setPhase(GameState.Phase.TERMINATED);
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
    // If turn wraps to the lead player, trick closes → new trick
    if (Objects.equals(s.currentPlayerId(), s.getCurrentLeadPlayer())) {
      s.setCurrentLead(null);
      s.setCurrentLeadPlayer(null);
      s.setPassesInRow(0);
    }
  }
}
