package com.yourco.ddz.engine.core;

/**
 * Canonical set of playable combinations in Dou Dizhu.
 */
public enum ComboType {
    SINGLE,                 // one card
    PAIR,                   // xx
    TRIPLE,                 // xxx
    TRIPLE_WITH_SINGLE,     // xxx + y
    TRIPLE_WITH_PAIR,       // xxx + yy

    SEQUENCE,               // x..y (consecutive ranks, length >= 5)
    PAIR_SEQUENCE,          // xx-yy-zz... (>= 3 pairs)
    AIRPLANE,               // consecutive triples
    AIRPLANE_WITH_SINGLES,  // airplane + equal singles
    AIRPLANE_WITH_PAIRS,    // airplane + equal pairs

    BOMB,                   // xxxx
    ROCKET                  // little joker + big joker
}