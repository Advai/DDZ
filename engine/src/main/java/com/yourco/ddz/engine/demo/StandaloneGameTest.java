package com.yourco.ddz.engine.demo;

import com.yourco.ddz.engine.cards.Card;
import com.yourco.ddz.engine.core.*;
import java.util.*;

/**
 * Standalone test for a complete DDZ game without server infrastructure.
 *
 * <p>Demonstrates full game flow: - Create game with N players - Run bidding phase - Handle
 * landlord selection (interactive) - Play until completion - Display final scores
 */
public class StandaloneGameTest {
  private static final Scanner scanner = new Scanner(System.in);

  public static void main(String[] args) {
    System.out.println("=== Dou Dizhu Standalone Game Test ===\n");

    // Get game configuration
    System.out.print("Enter number of players (3-12): ");
    int playerCount = Integer.parseInt(scanner.nextLine());

    if (playerCount < 3 || playerCount > 12) {
      System.out.println("Invalid player count. Must be 3-12.");
      return;
    }

    // Create players with names
    List<UUID> playerIds = new ArrayList<>();
    Map<UUID, String> playerNames = new HashMap<>();

    for (int i = 0; i < playerCount; i++) {
      System.out.print("Enter name for Player " + (i + 1) + ": ");
      String name = scanner.nextLine();
      UUID id = UUID.randomUUID();
      playerIds.add(id);
      playerNames.put(id, name);
    }

    // Create game
    GameConfig config = GameConfig.standard(playerCount);
    System.out.println("\n=== Game Configuration ===");
    System.out.println(config);
    System.out.println();

    GameState state = new GameState("test-game", playerIds);
    for (UUID id : playerIds) {
      state.setPlayerName(id, playerNames.get(id));
    }

    DdzRules rules = new DdzRules(config);
    GameLoop loop = new GameLoop(rules, state);

    // Start game (deal cards, enter BIDDING)
    System.out.println("Starting game...\n");
    loop.submit(new SystemAction("START", null));
    loop.tick();

    // Show hands
    System.out.println("=== Hands Dealt ===");
    for (UUID playerId : playerIds) {
      System.out.println(
          playerNames.get(playerId) + ": " + formatHand(state.handOf(playerId)) + "\n");
    }

    // Bidding phase
    runBiddingPhase(loop, state, playerNames, config.getMaxBid());

    // Play phase
    runPlayPhase(loop, state, playerNames);

    // Show final results
    showFinalResults(state, playerNames);
  }

  private static void runBiddingPhase(
      GameLoop loop, GameState state, Map<UUID, String> playerNames, int maxBid) {
    System.out.println("\n=== BIDDING PHASE ===");
    System.out.println("Max bid: " + maxBid);
    System.out.println("Bid 0 to pass, 1-" + maxBid + " to bid\n");

    while (state.phase() == GameState.Phase.BIDDING) {
      UUID currentPlayer = state.currentPlayerId();
      String playerName = playerNames.get(currentPlayer);

      // Show bids placed so far
      if (!state.getAllBids().isEmpty()) {
        System.out.println("Bids placed so far:");
        for (Map.Entry<UUID, Integer> entry : state.getAllBids().entrySet()) {
          String name = playerNames.get(entry.getKey());
          int bid = entry.getValue();
          System.out.println("  " + name + ": " + bid + (bid == 0 ? " (passed)" : ""));
        }
        System.out.println("Current highest bid: " + state.getHighestBid());
      }

      System.out.print(playerName + ", enter your bid (0-" + maxBid + "): ");
      int bidValue = Integer.parseInt(scanner.nextLine());

      Bid bid = new Bid(bidValue);
      loop.submit(new PlayerAction(currentPlayer, "BID", bid));
      loop.tick();

      // Check if awaiting landlord selection
      handleLandlordSelection(loop, state, playerNames);
    }
  }

  private static void handleLandlordSelection(
      GameLoop loop, GameState state, Map<UUID, String> playerNames) {
    UUID awaitingSelection = state.getAwaitingLandlordSelection();

    if (awaitingSelection != null) {
      String selectorName = playerNames.get(awaitingSelection);
      List<UUID> selectedSoFar = new ArrayList<>(state.getSelectedLandlords());

      // Get eligible players (not already selected)
      List<UUID> eligible = new ArrayList<>(state.players());
      eligible.removeAll(selectedSoFar);

      System.out.println("\n=== LANDLORD SELECTION ===");
      System.out.println(selectorName + ", select your teammate:");

      for (int i = 0; i < eligible.size(); i++) {
        UUID playerId = eligible.get(i);
        System.out.println((i + 1) + ". " + playerNames.get(playerId));
      }

      System.out.print("Enter choice (1-" + eligible.size() + "): ");
      int choice = Integer.parseInt(scanner.nextLine()) - 1;

      if (choice >= 0 && choice < eligible.size()) {
        UUID selected = eligible.get(choice);

        // Submit landlord selection action
        loop.submit(new PlayerAction(awaitingSelection, "SELECT_LANDLORD", selected));
        loop.tick();

        System.out.println(
            selectorName + " selected " + playerNames.get(selected) + " as landlord!\n");

        // Recursively check if another selection is needed (for 3-landlord case)
        handleLandlordSelection(loop, state, playerNames);
      } else {
        System.out.println("Invalid choice, selecting randomly...");
        UUID randomSelection = eligible.get(new Random().nextInt(eligible.size()));
        loop.submit(new PlayerAction(awaitingSelection, "SELECT_LANDLORD", randomSelection));
        loop.tick();
        handleLandlordSelection(loop, state, playerNames);
      }
    }
  }

  private static void runPlayPhase(GameLoop loop, GameState state, Map<UUID, String> playerNames) {
    System.out.println("\n=== PLAY PHASE ===");
    System.out.println("Landlords: " + getPlayerNames(state.getLandlordIds(), playerNames));
    System.out.println();

    while (state.phase() == GameState.Phase.PLAY) {
      UUID currentPlayer = state.currentPlayerId();
      String playerName = playerNames.get(currentPlayer);

      // Check if player is disconnected (for testing, all are connected)
      if (!state.isPlayerConnected(currentPlayer)) {
        System.out.println(playerName + " is disconnected, auto-passing...");
        loop.submit(new PlayerAction(currentPlayer, "PLAY", null));
        loop.tick();
        continue;
      }

      System.out.println("--- " + playerName + "'s Turn ---");
      System.out.println("Your hand: " + formatHand(state.handOf(currentPlayer)));

      if (state.getCurrentLead() != null) {
        System.out.println(
            "Current lead: "
                + state.getCurrentLead().type()
                + " by "
                + playerNames.get(state.getCurrentLeadPlayer()));
        System.out.println("Cards: " + formatCards(state.getCurrentLead().cards()));
      } else {
        System.out.println("You lead this round!");
      }

      System.out.println("\nEnter cards to play (e.g., 3H,3D for pair of 3s), or 'PASS' to pass:");
      System.out.print("> ");
      String input = scanner.nextLine().trim();

      if (input.equalsIgnoreCase("PASS")) {
        // Pass
        loop.submit(new PlayerAction(currentPlayer, "PLAY", null));
      } else {
        // Parse and play cards
        List<Card> cards = parseCards(input);
        if (cards == null || cards.isEmpty()) {
          System.out.println("Invalid input, try again.");
          continue;
        }
        loop.submit(new PlayerAction(currentPlayer, "PLAY", cards));
      }

      try {
        loop.tick();
      } catch (Exception e) {
        System.out.println("ERROR: " + e.getMessage());
        System.out.println("Try again.\n");
        continue;
      }

      System.out.println();
    }
  }

  private static void showFinalResults(GameState state, Map<UUID, String> playerNames) {
    System.out.println("\n=== GAME OVER ===");

    if (state.phase() == GameState.Phase.TERMINATED) {
      // Find winner
      UUID winner = null;
      for (UUID p : state.players()) {
        if (state.handOf(p).isEmpty()) {
          winner = p;
          break;
        }
      }

      if (winner != null) {
        System.out.println("Winner: " + playerNames.get(winner));
        System.out.println(
            "Team: " + (state.isLandlord(winner) ? "LANDLORDS" : "FARMERS") + " WIN!");
      }

      System.out.println("\nFinal Scores:");
      for (UUID playerId : state.players()) {
        int score = state.getScores().getOrDefault(playerId, 0);
        String role = state.isLandlord(playerId) ? "(Landlord)" : "(Farmer)";
        System.out.println(playerNames.get(playerId) + " " + role + ": " + score);
      }

      System.out.println("\nBombs played: " + state.getBombsPlayed());
      System.out.println("Rockets played: " + state.getRocketsPlayed());
    }
  }

  // ===== Helper Methods =====

  private static String formatHand(List<Card> hand) {
    if (hand.isEmpty()) return "(empty)";
    return hand.size() + " cards: " + formatCards(hand);
  }

  private static String formatCards(List<Card> cards) {
    return cards.stream()
        .map(StandaloneGameTest::formatCard)
        .reduce((a, b) -> a + ", " + b)
        .orElse("");
  }

  private static String formatCard(Card card) {
    String rankStr = formatRank(card.rank());
    String suitStr = formatSuit(card.suit());
    return rankStr + suitStr;
  }

  private static String formatRank(Card.Rank rank) {
    return switch (rank) {
      case TEN -> "T";
      case JACK -> "J";
      case QUEEN -> "Q";
      case KING -> "K";
      case ACE -> "A";
      case LITTLE_JOKER -> "LJ";
      case BIG_JOKER -> "BJ";
      default -> rank.name().substring(0, 1);
    };
  }

  private static String formatSuit(Card.Suit suit) {
    return switch (suit) {
      case SPADES -> "♠";
      case HEARTS -> "♥";
      case DIAMONDS -> "♦";
      case CLUBS -> "♣";
      case JOKER -> "";
    };
  }

  private static List<Card> parseCards(String input) {
    try {
      String[] parts = input.split(",");
      List<Card> cards = new ArrayList<>();

      for (String part : parts) {
        part = part.trim().toUpperCase();
        if (part.isEmpty()) continue;

        Card.Rank rank = parseRank(part);
        Card.Suit suit = parseSuit(part);

        if (rank == null || suit == null) {
          System.out.println("Could not parse: " + part);
          return null;
        }

        cards.add(new Card(suit, rank));
      }

      return cards;
    } catch (Exception e) {
      return null;
    }
  }

  private static Card.Rank parseRank(String s) {
    if (s.startsWith("LJ")) return Card.Rank.LITTLE_JOKER;
    if (s.startsWith("BJ")) return Card.Rank.BIG_JOKER;

    char first = s.charAt(0);
    return switch (first) {
      case '2' -> Card.Rank.TWO;
      case '3' -> Card.Rank.THREE;
      case '4' -> Card.Rank.FOUR;
      case '5' -> Card.Rank.FIVE;
      case '6' -> Card.Rank.SIX;
      case '7' -> Card.Rank.SEVEN;
      case '8' -> Card.Rank.EIGHT;
      case '9' -> Card.Rank.NINE;
      case 'T' -> Card.Rank.TEN;
      case 'J' -> Card.Rank.JACK;
      case 'Q' -> Card.Rank.QUEEN;
      case 'K' -> Card.Rank.KING;
      case 'A' -> Card.Rank.ACE;
      default -> null;
    };
  }

  private static Card.Suit parseSuit(String s) {
    if (s.contains("LJ") || s.contains("BJ")) return Card.Suit.JOKER;

    char last = s.charAt(s.length() - 1);
    return switch (last) {
      case 'S', '♠' -> Card.Suit.SPADES;
      case 'H', '♥' -> Card.Suit.HEARTS;
      case 'D', '♦' -> Card.Suit.DIAMONDS;
      case 'C', '♣' -> Card.Suit.CLUBS;
      default -> null;
    };
  }

  private static String getPlayerNames(List<UUID> playerIds, Map<UUID, String> playerNames) {
    return playerIds.stream().map(playerNames::get).reduce((a, b) -> a + ", " + b).orElse("");
  }
}
