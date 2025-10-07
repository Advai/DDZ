package com.yourco.ddz.engine.core;

import com.yourco.ddz.engine.cards.Card;

/** Minimal stub: always returns 0 (equal). Replace with real comparison logic later. */
public final class SimplePlayComparator implements PlayComparator {
  @Override
  public int compare(PlayedHand a, PlayedHand b) {
    // TODO: implement comparison
    // check length
    if (a.cards().size() != b.cards().size()) {
      throw new IllegalStateException("Does not match size");
    }
    if (a.type() != b.type()) {
      throw new IllegalStateException("Does not match type");
    }
    int highest_a_rank = -1;
    int highest_b_rank = -1;
    for (Card c : a.cards()) {
      if (c.rank().ordinal() > highest_a_rank) {
        highest_a_rank = c.rank().ordinal();
      }
    }
    for (Card c : b.cards()) {
      if (c.rank().ordinal() > highest_b_rank) {
        highest_b_rank = c.rank().ordinal();
      }
    }
    return highest_a_rank > highest_b_rank ? 1 : 0;
  }
}
