package com.yourco.ddz.engine.core;

import com.yourco.ddz.engine.cards.Card;
import java.util.*;

/** Test utilities for creating cards, states, and game scenarios easily. */
public class TestUtils {

  /**
   * Create a card from rank and suit strings.
   *
   * @param rank "THREE", "FOUR", ..., "ACE", "TWO", "LITTLE_JOKER", "BIG_JOKER"
   * @param suit "CLUBS", "DIAMONDS", "HEARTS", "SPADES", "JOKER"
   */
  public static Card card(String rank, String suit) {
    return new Card(Card.Suit.valueOf(suit), Card.Rank.valueOf(rank));
  }

  /**
   * Create a card using short notation.
   *
   * @param notation "3H" = 3 of Hearts, "LJ" = Little Joker, "BJ" = Big Joker
   */
  public static Card card(String notation) {
    if (notation.equals("LJ")) {
      return new Card(Card.Suit.JOKER, Card.Rank.LITTLE_JOKER);
    }
    if (notation.equals("BJ")) {
      return new Card(Card.Suit.JOKER, Card.Rank.BIG_JOKER);
    }

    String rankStr = notation.substring(0, notation.length() - 1);
    String suitStr = notation.substring(notation.length() - 1);

    Card.Rank rank =
        switch (rankStr) {
          case "3" -> Card.Rank.THREE;
          case "4" -> Card.Rank.FOUR;
          case "5" -> Card.Rank.FIVE;
          case "6" -> Card.Rank.SIX;
          case "7" -> Card.Rank.SEVEN;
          case "8" -> Card.Rank.EIGHT;
          case "9" -> Card.Rank.NINE;
          case "T" -> Card.Rank.TEN;
          case "J" -> Card.Rank.JACK;
          case "Q" -> Card.Rank.QUEEN;
          case "K" -> Card.Rank.KING;
          case "A" -> Card.Rank.ACE;
          case "2" -> Card.Rank.TWO;
          default -> throw new IllegalArgumentException("Invalid rank: " + rankStr);
        };

    Card.Suit suit =
        switch (suitStr) {
          case "S" -> Card.Suit.SPADES;
          case "H" -> Card.Suit.HEARTS;
          case "D" -> Card.Suit.DIAMONDS;
          case "C" -> Card.Suit.CLUBS;
          default -> throw new IllegalArgumentException("Invalid suit: " + suitStr);
        };

    return new Card(suit, rank);
  }

  /**
   * Create multiple cards from notation.
   *
   * @param notations "3H", "3D", "3S", etc.
   */
  public static List<Card> cards(String... notations) {
    List<Card> result = new ArrayList<>();
    for (String notation : notations) {
      result.add(card(notation));
    }
    return result;
  }

  /**
   * Create a test game state with specified number of players.
   *
   * @param playerCount number of players (3-12)
   * @return GameState in LOBBY phase
   */
  public static GameState createTestState(int playerCount) {
    List<UUID> players = new ArrayList<>();
    for (int i = 0; i < playerCount; i++) {
      players.add(UUID.randomUUID());
    }
    GameState state = new GameState("test-game", players);

    // Set player names for easier debugging
    for (int i = 0; i < players.size(); i++) {
      state.setPlayerName(players.get(i), "Player" + (i + 1));
    }

    return state;
  }

  /**
   * Create a test game state with known player IDs.
   *
   * @param playerIds specific UUIDs to use
   * @return GameState in LOBBY phase
   */
  public static GameState createTestState(UUID... playerIds) {
    GameState state = new GameState("test-game", Arrays.asList(playerIds));

    for (int i = 0; i < playerIds.length; i++) {
      state.setPlayerName(playerIds[i], "Player" + (i + 1));
    }

    return state;
  }

  /**
   * Deal specific hands to players.
   *
   * @param state game state
   * @param hands map of player UUID to list of cards
   */
  public static void dealHands(GameState state, Map<UUID, List<Card>> hands) {
    for (Map.Entry<UUID, List<Card>> entry : hands.entrySet()) {
      state.handOf(entry.getKey()).clear();
      state.handOf(entry.getKey()).addAll(entry.getValue());
    }
  }

  /**
   * Deal same hand to a player using notation.
   *
   * @param state game state
   * @param playerId player to deal to
   * @param notations card notations
   */
  public static void dealHand(GameState state, UUID playerId, String... notations) {
    state.handOf(playerId).clear();
    state.handOf(playerId).addAll(cards(notations));
  }

  /**
   * Submit action and tick loop in one call.
   *
   * @param loop game loop
   * @param action action to submit
   */
  public static void submitAndTick(GameLoop loop, GameAction action) {
    loop.submit(action);
    loop.tick();
  }

  /**
   * Create a standard 3-player game in PLAY phase with specific landlord.
   *
   * @param landlordIndex index of landlord (0, 1, or 2)
   * @return configured game state
   */
  public static GameState create3PlayerGameInPlay(int landlordIndex) {
    GameState state = createTestState(3);
    state.setPhase(GameState.Phase.PLAY);

    UUID landlord = state.players().get(landlordIndex);
    state.setLandlordIds(List.of(landlord));
    state.setCurrentPlayerIndex(landlordIndex);

    return state;
  }

  /**
   * Create a test game config for specific player count.
   *
   * @param playerCount 3-12
   * @return game config
   */
  public static GameConfig createTestConfig(int playerCount) {
    return GameConfig.standard(playerCount);
  }
}
