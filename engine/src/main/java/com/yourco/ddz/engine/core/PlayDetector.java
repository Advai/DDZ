package com.yourco.ddz.engine.core;

import com.yourco.ddz.engine.cards.Card;
import java.util.List;
import java.util.Optional;

/**
 * Detects whether a raw set of cards is a valid combo.
 * Returns a normalized PlayedHand if valid, empty otherwise.
 */
public interface PlayDetector {
    Optional<PlayedHand> detect(List<Card> cards);
}