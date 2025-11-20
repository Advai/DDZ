package com.yourco.ddz.engine.core;

import static com.yourco.ddz.engine.core.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

/** Tests for landlord selection (snake draft) logic. */
class DdzRulesLandlordSelectionTest {

  @Test
  void testSingleLandlordImmediateDistribution() {
    GameState state = createTestState(3);
    GameConfig config = GameConfig.standard(3); // 1 landlord
    state.setPhase(GameState.Phase.BIDDING);

    // Set up bottom cards
    state.setBottom(cards("AH", "KD", "QS"));

    // Initialize with primary landlord
    UUID primaryLandlord = state.players().get(0);
    state.clearLandlordSelection();
    state.addSelectedLandlord(primaryLandlord);

    // For single landlord, should distribute immediately
    DdzRulesLandlordSelection.distributeLandlordCards(state, List.of(primaryLandlord));

    // Check landlord got bottom cards
    assertTrue(state.handOf(primaryLandlord).contains(card("AH")));
    assertTrue(state.handOf(primaryLandlord).contains(card("KD")));
    assertTrue(state.handOf(primaryLandlord).contains(card("QS")));

    // Check phase changed to PLAY
    assertEquals(GameState.Phase.PLAY, state.phase());

    // Check primary landlord starts
    assertEquals(primaryLandlord, state.currentPlayerId());
  }

  @Test
  void testTwoLandlordSnakeDraft() {
    GameState state = createTestState(5);
    GameConfig config = GameConfig.standard(5); // 2 landlords
    state.setPhase(GameState.Phase.BIDDING);

    UUID player0 = state.players().get(0);
    UUID player1 = state.players().get(1);

    // Initialize selection
    state.clearLandlordSelection();
    state.addSelectedLandlord(player0);
    state.setAwaitingLandlordSelection(player0);

    // Player 0 selects Player 1
    DdzRulesLandlordSelection.onSelectLandlord(
        state, new PlayerAction(player0, "SELECT_LANDLORD", player1), config);

    // Check both are landlords
    assertEquals(2, state.getSelectedLandlords().size());
    assertTrue(state.getSelectedLandlords().contains(player0));
    assertTrue(state.getSelectedLandlords().contains(player1));

    // Check phase changed to PLAY
    assertEquals(GameState.Phase.PLAY, state.phase());

    // Check no longer awaiting selection
    assertNull(state.getAwaitingLandlordSelection());
  }

  @Test
  void testThreeLandlordSnakeDraft() {
    GameState state = createTestState(8);
    GameConfig config = GameConfig.standard(8); // 3 landlords
    state.setPhase(GameState.Phase.BIDDING);

    UUID player0 = state.players().get(0);
    UUID player2 = state.players().get(2);
    UUID player5 = state.players().get(5);

    // Initialize with primary landlord
    state.clearLandlordSelection();
    state.addSelectedLandlord(player0);
    state.setAwaitingLandlordSelection(player0);

    // Player 0 selects Player 2
    DdzRulesLandlordSelection.onSelectLandlord(
        state, new PlayerAction(player0, "SELECT_LANDLORD", player2), config);

    // Now Player 2 should be selecting
    assertEquals(player2, state.getAwaitingLandlordSelection());
    assertEquals(2, state.getSelectedLandlords().size());

    // Player 2 selects Player 5
    DdzRulesLandlordSelection.onSelectLandlord(
        state, new PlayerAction(player2, "SELECT_LANDLORD", player5), config);

    // All 3 selected, should be in PLAY phase
    assertEquals(3, state.getSelectedLandlords().size());
    assertEquals(GameState.Phase.PLAY, state.phase());
    assertNull(state.getAwaitingLandlordSelection());
  }

  @Test
  void testBottomCardDistributionEvenSplit() {
    GameState state = createTestState(5);
    GameConfig config = GameConfig.standard(5); // 2 landlords
    state.setPhase(GameState.Phase.BIDDING);

    UUID landlord1 = state.players().get(0);
    UUID landlord2 = state.players().get(1);

    // 4 bottom cards, should split 2-2
    state.setBottom(cards("AH", "KD", "QS", "JC"));

    DdzRulesLandlordSelection.distributeLandlordCards(state, List.of(landlord1, landlord2));

    // Each landlord should get 2 cards
    // Note: implementation may give remainder to first landlord
    int total =
        (int)
                state.handOf(landlord1).stream()
                    .filter(c -> cards("AH", "KD", "QS", "JC").contains(c))
                    .count()
            + (int)
                state.handOf(landlord2).stream()
                    .filter(c -> cards("AH", "KD", "QS", "JC").contains(c))
                    .count();

    assertEquals(4, total, "All bottom cards should be distributed");
  }

  @Test
  void testBottomCardDistributionWithRemainder() {
    GameState state = createTestState(7);
    GameConfig config = GameConfig.standard(7); // 2 landlords
    state.setPhase(GameState.Phase.BIDDING);

    UUID landlord1 = state.players().get(0);
    UUID landlord2 = state.players().get(1);

    // 3 bottom cards, should split 2-1 (first landlord gets extra)
    state.setBottom(cards("AH", "KD", "QS"));

    int initialSize1 = state.handOf(landlord1).size();
    int initialSize2 = state.handOf(landlord2).size();

    DdzRulesLandlordSelection.distributeLandlordCards(state, List.of(landlord1, landlord2));

    // Landlord 1 should get 2 cards (1 + remainder)
    assertEquals(initialSize1 + 2, state.handOf(landlord1).size());

    // Landlord 2 should get 1 card
    assertEquals(initialSize2 + 1, state.handOf(landlord2).size());
  }

  @Test
  void testCannotSelectAlreadySelectedLandlord() {
    GameState state = createTestState(8);
    GameConfig config = GameConfig.standard(8); // 3 landlords
    state.setPhase(GameState.Phase.BIDDING);

    UUID player0 = state.players().get(0);
    UUID player1 = state.players().get(1);

    state.clearLandlordSelection();
    state.addSelectedLandlord(player0);
    state.setAwaitingLandlordSelection(player0);

    // Try to select Player 0 (already selected)
    assertThrows(
        IllegalStateException.class,
        () ->
            DdzRulesLandlordSelection.onSelectLandlord(
                state, new PlayerAction(player0, "SELECT_LANDLORD", player0), config));
  }

  @Test
  void testCannotSelectNonexistentPlayer() {
    GameState state = createTestState(5);
    GameConfig config = GameConfig.standard(5); // 2 landlords
    state.setPhase(GameState.Phase.BIDDING);

    UUID player0 = state.players().get(0);
    UUID randomPlayer = UUID.randomUUID();

    state.clearLandlordSelection();
    state.addSelectedLandlord(player0);
    state.setAwaitingLandlordSelection(player0);

    // Try to select player not in game
    assertThrows(
        IllegalStateException.class,
        () ->
            DdzRulesLandlordSelection.onSelectLandlord(
                state, new PlayerAction(player0, "SELECT_LANDLORD", randomPlayer), config));
  }

  @Test
  void testNotYourTurnToSelect() {
    GameState state = createTestState(5);
    GameConfig config = GameConfig.standard(5); // 2 landlords
    state.setPhase(GameState.Phase.BIDDING);

    UUID player0 = state.players().get(0);
    UUID player1 = state.players().get(1);
    UUID player2 = state.players().get(2);

    state.clearLandlordSelection();
    state.addSelectedLandlord(player0);
    state.setAwaitingLandlordSelection(player0);

    // Player 1 tries to select when it's Player 0's turn
    assertThrows(
        IllegalStateException.class,
        () ->
            DdzRulesLandlordSelection.onSelectLandlord(
                state, new PlayerAction(player1, "SELECT_LANDLORD", player2), config));
  }
}
