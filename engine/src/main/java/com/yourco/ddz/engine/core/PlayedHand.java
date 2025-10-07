package com.yourco.ddz.engine.core;

import com.yourco.ddz.engine.cards.Card;
import java.util.List;

/**
 * A normalized play (combo) that has already been validated
 * by a PlayDetector.
 */
public record PlayedHand(
        ComboType type,
        List<Card> cards,
        int keyStrength, // strength of the main rank (for comparisons)
        int length       // e.g. number of ranks in a sequence; 1 for singles/pairs/triples
) {
    @Override
    public String toString() {
        return type + " " + cards.toString();
    }
}