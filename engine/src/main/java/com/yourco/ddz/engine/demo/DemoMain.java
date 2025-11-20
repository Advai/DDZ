package com.yourco.ddz.engine.demo;

import com.yourco.ddz.engine.cards.Card;
import com.yourco.ddz.engine.core.*;
import java.util.*;

public class DemoMain {
  public static void main(String[] args) {
    // Init engine
    List<UUID> players = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    System.out.println("Past Player");
    Rules rules = DdzRules.standard3Player(); // Standard 3-player game
    System.out.println("Past Rules");
    GameState state = new GameState("g-1", players);
    GameLoop loop = new GameLoop(rules, state);
    Scanner scanner = new Scanner(System.in);

    // Start (deal, then enter BIDDING)
    loop.submit(new SystemAction("START", null));
    loop.tick();

    // Force bidding to 3 so we enter PLAY with the current player as landlord
    int numPlayersBid = 0;
    state.setPhase(GameState.Phase.BIDDING);
    while (state.phase() == GameState.Phase.BIDDING && numPlayersBid < 3) {
      UUID currPlayer = state.currentPlayerId();

      System.out.print("Enter BID value for player " + currPlayer + ": ");
      String bidValue = scanner.nextLine();
      Bid bid = new Bid(Integer.parseInt(bidValue));

      loop.submit(new PlayerAction(currPlayer, "BID", bid));
      loop.tick();

      System.out.println("Turn advanced. Current player: " + state.currentPlayerId());

      numPlayersBid++;
    }
    System.out.println("Current player is " + state.getCurrentLeadPlayer());
    System.out.println("Landlord is " + state.getLandlordId());

    // PLAY loop: submit next action for the current player, tick, repeat
    // NOTE: With detector stubbed, any non-null card list will be invalid.
    // This loop is the structure you’ll keep; plug real move selection once detector works.
    state.setPhase(GameState.Phase.PLAY);
    while (!rules.isTerminal(state)) {
      UUID pid = state.currentPlayerId();
      //    for (UUID pid : state.players()) {
      // UUID pid = state.currentPlayerId();

      // Decide the move to submit:
      // - null payload = PASS (allowed only if there is a current lead)
      // - List<Card> payload = attempt a play (requires a working detector)
      List<Card> move = chooseNextMove(state, pid, scanner);
      System.out.println("Player " + pid + " plays " + move);
      try {
        loop.submit(new PlayerAction(pid, "PLAY", move)); // null => PASS
        loop.tick();
      } catch (RuntimeException ex) {
        // In production, handle NACK and re-prompt; here we log and break to avoid a tight loop.
        System.out.println("Rejected move for " + pid + ": " + ex.getMessage());
        break;
      }

      // Optional minimal telemetry
      //      System.out.println(
      //          "Turn advanced. Current player: "
      //              + state.currentPlayerId()
      //              + ", lead="
      //              + (state.getCurrentLead() == null ? "none" : state.getCurrentLead().type())
      //              + ", p0="
      //              + state.handOf(players.get(0)).size()
      //              + ", p1="
      //              + state.handOf(players.get(1)).size()
      //              + ", p2="
      //              + state.handOf(players.get(2)).size());
    }

    System.out.println("Phase: " + state.phase());
  }

  // For now, this is a placeholder. Once your detector works, replace this with:
  // - if no lead: return a legal opening combo (e.g., lowest SINGLE or PAIR)
  // - else: try to beat current lead; otherwise return null (PASS)
  private static List<Card> chooseNextMove(GameState state, UUID pid, Scanner scanner) {
    // Until detection is implemented, avoid playing cards.
    // If there is no current lead, PASS is illegal for the leader, so return null only when lead
    // exists.
    //    if (state.getCurrentLead() == null) {
    //      return Collections.singletonList(state.handOf(pid).getFirst());
    //    } else {
    //      if (state.handOf(pid).getLast().rank().ordinal()
    //          <= state.getCurrentLead().cards().getLast().rank().ordinal()) {
    //        return null;
    //      } else {
    //        return Collections.singletonList(state.handOf(pid).getLast());
    //      }
    //    }
    System.out.println(pid + ": It is your turn. Your hand is: " + state.handOf(pid));
    System.out.println(
        "Input your hand as a no-space comma separated list of hand abbreviations like 7H, 5S, 3D, JC (S for Spades, H for Hearts, D for Diamonds, and C for Clubs)");
    System.out.println(
        "Write T for 10 (like TD for 10 of diamonds), LJ for LITTLE_JOKER, and BJ for BIG_JOKER");
    System.out.println("Type PASS to play no cards.");
    String hand = scanner.nextLine();
    if (hand.equals("PASS")) {
      return null;
    }
    String[] hand_split = hand.split(",");
    ArrayList<Card> hand_ret = new ArrayList<Card>();
    for (String card : hand_split) {
      Card.Rank rank;
      Card.Suit suit;
      switch (card.charAt(0)) {
        case '2' -> rank = Card.Rank.TWO;
        case '3' -> rank = Card.Rank.THREE;
        case '4' -> rank = Card.Rank.FOUR;
        case '5' -> rank = Card.Rank.FIVE;
        case '6' -> rank = Card.Rank.SIX;
        case '7' -> rank = Card.Rank.SEVEN;
        case '8' -> rank = Card.Rank.EIGHT;
        case '9' -> rank = Card.Rank.NINE;
        case 'T' -> rank = Card.Rank.TEN;
        case 'J' -> rank = Card.Rank.JACK;
        case 'Q' -> rank = Card.Rank.QUEEN;
        case 'K' -> rank = Card.Rank.KING;
        case 'A' -> rank = Card.Rank.ACE;
        case 'L' -> rank = Card.Rank.LITTLE_JOKER;
        case 'B' -> rank = Card.Rank.BIG_JOKER;
        default -> rank = null;
      }
      switch (card.charAt(1)) {
        case 'S' -> suit = Card.Suit.SPADES;
        case 'D' -> suit = Card.Suit.DIAMONDS;
        case 'H' -> suit = Card.Suit.HEARTS;
        case 'C' -> suit = Card.Suit.CLUBS;
        default -> suit = null;
      }
      hand_ret.add(new Card(suit, rank));
    }
    return hand_ret;

    //    return (state.getCurrentLead() != null) ? null /* PASS */ : null /* would need a real play
    // */;
  }
}
