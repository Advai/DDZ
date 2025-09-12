package com.yourco.ddz.engine.cards;
import java.util.*; 
public final class Deck {
  private final List<Card> cards = new ArrayList<>();
  public Deck(){
    for (Card.Suit s : new Card.Suit[]{Card.Suit.CLUBS, Card.Suit.DIAMONDS, Card.Suit.HEARTS, Card.Suit.SPADES}) {
      for (Card.Rank r : new Card.Rank[]{Card.Rank.THREE, Card.Rank.FOUR, Card.Rank.FIVE, Card.Rank.SIX, Card.Rank.SEVEN, Card.Rank.EIGHT, Card.Rank.NINE, Card.Rank.TEN, Card.Rank.JACK, Card.Rank.QUEEN, Card.Rank.KING, Card.Rank.ACE, Card.Rank.TWO}) {
        cards.add(new Card(s, r));
      }
    }
    cards.add(new Card(Card.Suit.JOKER, Card.Rank.LITTLE_JOKER));
    cards.add(new Card(Card.Suit.JOKER, Card.Rank.BIG_JOKER));
  }
  public List<Card> asList(){ return Collections.unmodifiableList(cards); }
  public void shuffle(){ Collections.shuffle(cards); }
}