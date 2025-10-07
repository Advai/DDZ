package com.yourco.ddz.engine.core;

import com.yourco.ddz.engine.cards.Card;
import java.util.List;
import java.util.Optional;

/** Minimal stub: always returns empty. Replace with real detection logic later. */
public final class SimplePlayDetector implements PlayDetector {
  @Override
  public Optional<PlayedHand> detect(List<Card> cards) {
    // TODO: implement detection
    ComboType type = ComboType.SINGLE;
    return Optional.of(new PlayedHand(type, cards));
  }
}
