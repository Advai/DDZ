package com.yourco.ddz.engine.cards;

import java.util.Objects;

public final class Card {
  public enum Suit {
    CLUBS,
    DIAMONDS,
    HEARTS,
    SPADES,
    JOKER
  }

  public enum Rank {
    THREE,
    FOUR,
    FIVE,
    SIX,
    SEVEN,
    EIGHT,
    NINE,
    TEN,
    JACK,
    QUEEN,
    KING,
    ACE,
    TWO,
    LITTLE_JOKER,
    BIG_JOKER
  }

  private final Suit suit;
  private final Rank rank;

  public Card(Suit suit, Rank rank) {
    this.suit = suit;
    this.rank = rank;
  }

  public Suit suit() {
    return suit;
  }

  public Rank rank() {
    return rank;
  }

  @Override
  public String toString() {
    return rank + " of " + suit;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Card)) return false;
    Card c = (Card) o;
    return suit == c.suit && rank == c.rank;
  }

  @Override
  public int hashCode() {
    return Objects.hash(suit, rank);
  }
}
