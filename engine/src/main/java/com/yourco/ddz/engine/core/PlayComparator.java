package com.yourco.ddz.engine.core;

/**
 * Compares two already-detected PlayedHands.
 * >0 if a beats b, 0 if equal, <0 if loses. May throw if incomparable.
 */
public interface PlayComparator {
    int compare(PlayedHand a, PlayedHand b);
}