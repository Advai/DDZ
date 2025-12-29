package com.yourco.ddz.engine.core;

import com.yourco.ddz.engine.cards.Card;
import java.util.*;

/** Landlord selection logic for DdzRules */
public class DdzRulesLandlordSelection {

  public static void onSelectLandlord(GameState s, PlayerAction pa, GameConfig config) {
    UUID selector = s.getAwaitingLandlordSelection();

    if (!pa.playerId().equals(selector)) {
      throw new IllegalStateException("Not your turn to select a landlord");
    }

    if (!(pa.payload() instanceof UUID selectedPlayer)) {
      throw new IllegalArgumentException("Payload must be a UUID (selected player)");
    }

    // Validate selection
    if (s.getSelectedLandlords().contains(selectedPlayer)) {
      throw new IllegalStateException("Player already selected as landlord");
    }

    if (!s.players().contains(selectedPlayer)) {
      throw new IllegalStateException("Selected player not in game");
    }

    // Add to landlord team
    s.addSelectedLandlord(selectedPlayer);

    System.out.println(
        s.getPlayerName(selector)
            + " selected "
            + s.getPlayerName(selectedPlayer)
            + " as landlord");

    // Check if we need more landlords
    if (s.getSelectedLandlords().size() < config.getLandlordCount()) {
      // Snake draft: next selector is the player we just selected
      s.setAwaitingLandlordSelection(selectedPlayer);
      System.out.println("Awaiting selection from " + s.getPlayerName(selectedPlayer) + "...");
    } else {
      // All landlords selected, finalize team
      s.setAwaitingLandlordSelection(null);
      List<UUID> landlords = new ArrayList<>(s.getSelectedLandlords());

      System.out.println("Final landlord team: ");
      for (UUID landlord : landlords) {
        System.out.println("  - " + s.getPlayerName(landlord));
      }
      System.out.println("================================");

      // Distribute cards and start play
      distributeLandlordCards(s, landlords);
    }
  }

  public static void distributeLandlordCards(GameState s, List<UUID> landlords) {
    s.setLandlordIds(landlords);

    System.out.println("\n=== BOTTOM CARD DISTRIBUTION ===");
    System.out.println("Landlords (" + landlords.size() + " total):");
    for (UUID landlord : landlords) {
      System.out.println("  - " + s.getPlayerName(landlord));
    }

    // Distribute bottom cards evenly among landlords
    var bottom = new ArrayList<>(s.bottom());
    System.out.println("Bottom cards (" + bottom.size() + " total): " + bottom);

    int cardsPerLandlord = bottom.size() / landlords.size();
    int remainder = bottom.size() % landlords.size();
    System.out.println(
        "Distribution: " + cardsPerLandlord + " cards per landlord, " + remainder + " extra");

    for (int i = 0; i < landlords.size(); i++) {
      UUID landlord = landlords.get(i);
      int cardsToGive = cardsPerLandlord + (i < remainder ? 1 : 0);

      System.out.println(
          "\n--- Distributing to "
              + s.getPlayerName(landlord)
              + " (landlord "
              + (i + 1)
              + "/"
              + landlords.size()
              + ") ---");
      System.out.println("Cards to give: " + cardsToGive);
      System.out.println("Cards remaining in bottom pile: " + bottom.size());

      List<Card> landlordBottomCards = new ArrayList<>();
      for (int j = 0; j < cardsToGive; j++) {
        if (!bottom.isEmpty()) {
          landlordBottomCards.add(bottom.remove(0));
        }
      }

      int handSizeBefore = s.handOf(landlord).size();
      s.handOf(landlord).addAll(landlordBottomCards);
      // Track which cards are bottom cards for this landlord
      s.setLandlordBottomCards(landlord, landlordBottomCards);
      Collections.sort(s.handOf(landlord), Comparator.comparing(Card::rank));
      int handSizeAfter = s.handOf(landlord).size();

      System.out.println("Cards given: " + landlordBottomCards);
      System.out.println("Hand size: " + handSizeBefore + " -> " + handSizeAfter);
    }

    System.out.println("\nBottom cards remaining: " + bottom.size() + " (should be 0)");

    // Primary landlord (first in list) starts the game
    UUID primaryLandlord = landlords.get(0);
    int landlordIndex = s.players().indexOf(primaryLandlord);
    s.setCurrentPlayerIndex(landlordIndex);
    s.setCurrentLead(null);
    s.setCurrentLeadPlayer(null);
    s.setPassesInRow(0);

    System.out.println(s.getPlayerName(primaryLandlord) + " starts the game");
    System.out.println("=================================\n");

    // Transition to PLAY phase
    s.setPhase(GameState.Phase.PLAY);
  }
}
