package com.yourco.ddz.engine.core;

import com.yourco.ddz.engine.cards.Card;
import java.util.List;
import java.util.Optional;

/** Minimal stub: always returns empty. Replace with real detection logic later. */
public final class SimplePlayDetector implements PlayDetector {
  @Override
  public Optional<PlayedHand> detect(List<Card> cards) {
    // TODO: implement detection
    ComboType type;
    switch (cards.size()) {
      case 1:
        type = ComboType.SINGLE;
      case 2:
        type = ComboType.PAIR;
      case 3:
        type = ComboType.TRIPLE;
      case 4:
        // when 4, could be bomb or trip with kicker.
        if (cards.stream().map(Card::rank).count() == 4) {
          type = ComboType.BOMB;
        } else if (cards.stream().map(Card::rank).count() == 3) {
          type = ComboType.TRIPLE_WITH_SINGLE;
        }
      case 5:
      // 5 is full house

      // 6 is airplane, bomb with 2 kickers
      default:
        type = null;
    }
    return Optional.of(new PlayedHand(type, cards));
  }
}
