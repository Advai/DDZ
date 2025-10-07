package com.yourco.ddz.engine.core;

import com.yourco.ddz.engine.cards.Card;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class Presets {
  private Presets() {}

  /** Detector settings for classic 3-player Dou Dizhu. */
  public static DetectorConfig ddz3pDetector() {
    return new DetectorConfig(
        Set.of(ComboType.values()), // allow all types for now; tighten later if you want
        5, // min straight length
        3, // min pair-straight length
        false, // 2 not allowed in straights
        false // jokers not allowed in straights
        );
  }

  /** Comparator settings: rank order 3..A < 2 < littleJoker < bigJoker. */
  public static ComparatorConfig ddzComparator() {
    Map<Card.Rank, Integer> str = new EnumMap<>(Card.Rank.class);
    int i = 1;
    str.put(Card.Rank.THREE, i++);
    str.put(Card.Rank.FOUR, i++);
    str.put(Card.Rank.FIVE, i++);
    str.put(Card.Rank.SIX, i++);
    str.put(Card.Rank.SEVEN, i++);
    str.put(Card.Rank.EIGHT, i++);
    str.put(Card.Rank.NINE, i++);
    str.put(Card.Rank.TEN, i++);
    str.put(Card.Rank.JACK, i++);
    str.put(Card.Rank.QUEEN, i++);
    str.put(Card.Rank.KING, i++);
    str.put(Card.Rank.ACE, i++);
    str.put(Card.Rank.TWO, i++);
    str.put(Card.Rank.LITTLE_JOKER, i++);
    str.put(Card.Rank.BIG_JOKER, i++);
    return new ComparatorConfig(
        str,
        true, // bombs beat any non-bomb
        true // rocket enabled
        );
  }
}
