package com.yourco.ddz.engine.demo;

import com.yourco.ddz.engine.cards.Card;
import com.yourco.ddz.engine.core.*;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class DemoMain {
  public static void main(String[] args) {
    // Init engine
    List<UUID> players = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    Rules rules =
        com.yourco.ddz.engine.demo.DdzRules3p
            .withStubs(); // uses SimplePlayDetector/Comparator stubs
    GameState state = new GameState("g-1", players);
    GameLoop loop = new GameLoop(rules, state);

    // Start (deal, then enter BIDDING)
    loop.submit(new SystemAction("START", null));
    loop.tick();

    // Force bidding to 3 so we enter PLAY with the current player as landlord
    if (state.phase() == GameState.Phase.BIDDING) {
      loop.submit(new PlayerAction(state.currentPlayerId(), "BID", new Object()));
      loop.tick();
    }

    // PLAY loop: submit next action for the current player, tick, repeat
    // NOTE: With detector stubbed, any non-null card list will be invalid.
    // This loop is the structure you’ll keep; plug real move selection once detector works.
    while (!rules.isTerminal(state)) {
      UUID pid = state.currentPlayerId();

      // Decide the move to submit:
      // - null payload = PASS (allowed only if there is a current lead)
      // - List<Card> payload = attempt a play (requires a working detector)
      List<Card> move = chooseNextMove(state, pid);
//      System.out.println("Player " + pid + " plays " + move);
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
  private static List<Card> chooseNextMove(GameState state, UUID pid) {
    // Until detection is implemented, avoid playing cards.
    // If there is no current lead, PASS is illegal for the leader, so return null only when lead
    // exists.
    if (state.getCurrentLead() == null) {
      return Collections.singletonList(state.handOf(pid).getFirst());
    } else {
      if (state.handOf(pid).getLast().rank().ordinal()
          <= state.getCurrentLead().cards().getLast().rank().ordinal()) {
        return null;
      } else {
        return Collections.singletonList(state.handOf(pid).getLast());
      }
    }
    //    return (state.getCurrentLead() != null) ? null /* PASS */ : null /* would need a real play
    // */;
  }
}
