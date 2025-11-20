package com.yourco.ddz.engine.core;

import static com.yourco.ddz.engine.core.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

/** Tests for bidding phase logic. */
class DdzRulesBiddingTest {

  @Test
  void testAllPlayersBidDifferentValues() {
    GameState state = createTestState(3);
    GameConfig config = GameConfig.standard(3);
    state.setPhase(GameState.Phase.BIDDING);
    state.setCurrentPlayerIndex(0);

    // Player 0 bids 1
    DdzRulesBidding.onBid(
        state, new PlayerAction(state.currentPlayerId(), "BID", new Bid(1)), config);
    assertEquals(1, state.getPlayerBid(state.players().get(0)));

    // Player 1 bids 3
    DdzRulesBidding.onBid(
        state, new PlayerAction(state.currentPlayerId(), "BID", new Bid(3)), config);
    assertEquals(3, state.getPlayerBid(state.players().get(1)));

    // Player 2 bids 2
    DdzRulesBidding.onBid(
        state, new PlayerAction(state.currentPlayerId(), "BID", new Bid(2)), config);
    assertEquals(2, state.getPlayerBid(state.players().get(2)));

    // All bids collected
    assertTrue(state.hasEveryoneBid());
    assertEquals(3, state.getHighestBid());
  }

  @Test
  void testMultipleMaxBidders() {
    GameState state = createTestState(3);
    GameConfig config = GameConfig.standard(3);
    state.setPhase(GameState.Phase.BIDDING);
    state.setCurrentPlayerIndex(0);

    // Player 0 bids max (3)
    DdzRulesBidding.onBid(
        state, new PlayerAction(state.currentPlayerId(), "BID", new Bid(3)), config);

    // Player 1 bids 0
    DdzRulesBidding.onBid(
        state, new PlayerAction(state.currentPlayerId(), "BID", new Bid(0)), config);

    // Player 2 bids max (3)
    DdzRulesBidding.onBid(
        state, new PlayerAction(state.currentPlayerId(), "BID", new Bid(3)), config);

    // Should have 2 highest bidders
    List<UUID> highestBidders = state.getHighestBidders();
    assertEquals(2, highestBidders.size());
    assertTrue(highestBidders.contains(state.players().get(0)));
    assertTrue(highestBidders.contains(state.players().get(2)));
  }

  @Test
  void testEveryoneBidsZero() {
    GameState state = createTestState(3);
    GameConfig config = GameConfig.standard(3);
    state.setPhase(GameState.Phase.BIDDING);
    state.setCurrentPlayerIndex(0);

    // All players pass
    for (int i = 0; i < 3; i++) {
      DdzRulesBidding.onBid(
          state, new PlayerAction(state.currentPlayerId(), "BID", new Bid(0)), config);
    }

    // Everyone bid 0, highest should be 0
    assertEquals(0, state.getHighestBid());
    assertTrue(state.getHighestBidders().isEmpty()); // No one bid > 0
  }

  @Test
  void testSingleHighestBidder() {
    GameState state = createTestState(4);
    GameConfig config = GameConfig.standard(4);
    state.setPhase(GameState.Phase.BIDDING);
    state.setCurrentPlayerIndex(0);

    DdzRulesBidding.onBid(
        state, new PlayerAction(state.currentPlayerId(), "BID", new Bid(2)), config);
    DdzRulesBidding.onBid(
        state, new PlayerAction(state.currentPlayerId(), "BID", new Bid(1)), config);
    DdzRulesBidding.onBid(
        state, new PlayerAction(state.currentPlayerId(), "BID", new Bid(0)), config);
    DdzRulesBidding.onBid(
        state, new PlayerAction(state.currentPlayerId(), "BID", new Bid(1)), config);

    List<UUID> highestBidders = state.getHighestBidders();
    assertEquals(1, highestBidders.size());
    assertEquals(state.players().get(0), highestBidders.get(0));
  }

  @Test
  void testBidValidationMinValue() {
    GameState state = createTestState(3);
    GameConfig config = GameConfig.standard(3);
    state.setPhase(GameState.Phase.BIDDING);

    // Bid 0 (pass) is valid
    assertDoesNotThrow(
        () ->
            DdzRulesBidding.onBid(
                state, new PlayerAction(state.currentPlayerId(), "BID", new Bid(0)), config));
  }

  @Test
  void testBidValidationMaxValue() {
    GameState state = createTestState(3);
    GameConfig config = GameConfig.standard(3); // max bid = 3
    state.setPhase(GameState.Phase.BIDDING);

    // Bid 3 (max) is valid
    assertDoesNotThrow(
        () ->
            DdzRulesBidding.onBid(
                state, new PlayerAction(state.currentPlayerId(), "BID", new Bid(3)), config));
  }

  @Test
  void testInvalidBidNegative() {
    GameState state = createTestState(3);
    GameConfig config = GameConfig.standard(3);
    state.setPhase(GameState.Phase.BIDDING);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            DdzRulesBidding.onBid(
                state, new PlayerAction(state.currentPlayerId(), "BID", new Bid(-1)), config));
  }

  @Test
  void testInvalidBidExceedsMax() {
    GameState state = createTestState(3);
    GameConfig config = GameConfig.standard(3); // max bid = 3
    state.setPhase(GameState.Phase.BIDDING);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            DdzRulesBidding.onBid(
                state, new PlayerAction(state.currentPlayerId(), "BID", new Bid(4)), config));
  }

  @Test
  void testNotYourTurn() {
    GameState state = createTestState(3);
    GameConfig config = GameConfig.standard(3);
    state.setPhase(GameState.Phase.BIDDING);
    state.setCurrentPlayerIndex(0);

    // Player 1 tries to bid when it's Player 0's turn
    UUID wrongPlayer = state.players().get(1);

    assertThrows(
        IllegalStateException.class,
        () ->
            DdzRulesBidding.onBid(state, new PlayerAction(wrongPlayer, "BID", new Bid(2)), config));
  }

  @Test
  void testBiddingAdvancesPlayerIndex() {
    GameState state = createTestState(3);
    GameConfig config = GameConfig.standard(3);
    state.setPhase(GameState.Phase.BIDDING);
    state.setCurrentPlayerIndex(0);

    assertEquals(0, state.currentPlayerIndex());

    DdzRulesBidding.onBid(
        state, new PlayerAction(state.currentPlayerId(), "BID", new Bid(1)), config);
    assertEquals(1, state.currentPlayerIndex());

    DdzRulesBidding.onBid(
        state, new PlayerAction(state.currentPlayerId(), "BID", new Bid(2)), config);
    assertEquals(2, state.currentPlayerIndex());

    // After final bid, phase transitions and player index may change to landlord
    // Just verify all bids are collected
    DdzRulesBidding.onBid(
        state, new PlayerAction(state.currentPlayerId(), "BID", new Bid(0)), config);
    assertTrue(state.hasEveryoneBid());
  }
}
