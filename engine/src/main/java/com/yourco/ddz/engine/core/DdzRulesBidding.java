package com.yourco.ddz.engine.core;

import java.util.*;

/** Bidding logic for DdzRules - extract for easier editing */
public class DdzRulesBidding {

  public static void onBid(GameState s, GameAction a, GameConfig config) {
    if (!(a instanceof PlayerAction pa)) {
      throw new IllegalArgumentException("Expected PlayerAction");
    }
    if (!pa.playerId().equals(s.currentPlayerId())) {
      throw new IllegalStateException("Not your turn");
    }

    // Expect Bid as the raw move
    if (!(pa.payload() instanceof Bid bid)) {
      throw new IllegalArgumentException("Bad payload");
    }

    int value = bid.getValue();
    if (value < 0 || value > config.getMaxBid()) {
      throw new IllegalArgumentException("Bid must be between 0 and " + config.getMaxBid());
    }

    // Record the player's bid
    s.setPlayerBid(pa.playerId(), value);
    System.out.println(
        s.getPlayerName(pa.playerId()) + " bid " + value + (value == 0 ? " (passed)" : ""));

    // Move to next player
    s.nextPlayer();

    // Check if everyone has bid
    if (s.hasEveryoneBid()) {
      System.out.println("\n=== ALL BIDS RECEIVED ===");
      displayAllBids(s);
      resolveLandlordsFromBids(s, config);
    }
  }

  private static void displayAllBids(GameState s) {
    System.out.println("Player bids:");
    for (UUID playerId : s.players()) {
      int bid = s.getPlayerBid(playerId);
      System.out.println(
          "  " + s.getPlayerName(playerId) + ": " + bid + (bid == 0 ? " (passed)" : ""));
    }
    System.out.println("Highest bid: " + s.getHighestBid());
    System.out.println("========================\n");
  }

  public static void resolveLandlordsFromBids(GameState s, GameConfig config) {
    List<UUID> highestBidders = s.getHighestBidders();

    if (highestBidders.isEmpty()) {
      // Everyone passed, give landlord to first player by default
      System.out.println("Everyone passed. First player becomes landlord by default.");
      UUID firstPlayer = s.players().get(0);
      initializeLandlordSelection(s, firstPlayer, config);
      return;
    }

    UUID primaryLandlord;

    if (highestBidders.size() == 1) {
      primaryLandlord = highestBidders.get(0);
      System.out.println("\n========================================");
      System.out.println("PRIMARY LANDLORD: " + s.getPlayerName(primaryLandlord));
      System.out.println("(Single highest bidder)");
      System.out.println("========================================\n");
    } else {
      // Multiple players with highest bid - random selection
      Random random = new Random();
      int winnerIndex = random.nextInt(highestBidders.size());
      primaryLandlord = highestBidders.get(winnerIndex);

      System.out.println("\n========================================");
      System.out.println("=== BID TIE - RANDOM SELECTION ===");
      System.out.println("Highest bidders (" + s.getHighestBid() + " points):");
      for (UUID bidder : highestBidders) {
        System.out.println("  - " + s.getPlayerName(bidder));
      }
      System.out.println("\nPRIMARY LANDLORD: " + s.getPlayerName(primaryLandlord));
      System.out.println("(Selected randomly)");
      System.out.println("========================================\n");
    }

    initializeLandlordSelection(s, primaryLandlord, config);
  }

  private static void initializeLandlordSelection(
      GameState s, UUID primaryLandlord, GameConfig config) {
    // Initialize landlord selection state
    s.clearLandlordSelection();
    s.addSelectedLandlord(primaryLandlord);

    if (config.getLandlordCount() == 1) {
      // Only one landlord needed - finalize immediately
      DdzRulesLandlordSelection.distributeLandlordCards(s, List.of(primaryLandlord));
    } else {
      // Multiple landlords needed - start interactive selection
      s.setAwaitingLandlordSelection(primaryLandlord);

      System.out.println("=== LANDLORD TEAM SELECTION ===");
      System.out.println("Primary landlord: " + s.getPlayerName(primaryLandlord));
      System.out.println(
          "Awaiting selection of " + (config.getLandlordCount() - 1) + " more landlords");
      System.out.println("================================");
    }
  }
}
