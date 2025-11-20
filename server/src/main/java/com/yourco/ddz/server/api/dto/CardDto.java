package com.yourco.ddz.server.api.dto;

import com.yourco.ddz.engine.cards.Card;

public record CardDto(String suit, String rank) {
  public static CardDto from(Card card) {
    return new CardDto(card.suit().name(), card.rank().name());
  }

  public Card toCard() {
    return new Card(Card.Suit.valueOf(suit), Card.Rank.valueOf(rank));
  }
}
