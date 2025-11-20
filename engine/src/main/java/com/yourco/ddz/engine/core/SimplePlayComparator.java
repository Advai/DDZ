package com.yourco.ddz.engine.core;

import com.yourco.ddz.engine.cards.Card;
import java.util.Comparator;

/**
 * DDZ Play Comparator implementing proper Dou Dizhu ranking rules.
 *
 * <p>Rules:
 *
 * <ul>
 *   <li>ROCKET (both jokers) beats everything
 *   <li>BOMB beats all non-bombs
 *   <li>Bombs are compared by rank (higher rank wins)
 *   <li>Non-bombs must match type and size to be comparable
 *   <li>Same type hands compared by highest card rank
 * </ul>
 */
public final class SimplePlayComparator implements PlayComparator {
  @Override
  public int compare(PlayedHand a, PlayedHand b) {
    // ROCKET beats everything (including other rockets, though that's impossible)
    if (a.type() == ComboType.ROCKET && b.type() == ComboType.ROCKET) {
      return 0; // Both rockets (impossible in real game)
    }
    if (a.type() == ComboType.ROCKET) {
      return 1; // a wins
    }
    if (b.type() == ComboType.ROCKET) {
      return -1; // b wins
    }

    // BOMB vs BOMB: compare by rank
    if (a.type() == ComboType.BOMB && b.type() == ComboType.BOMB) {
      return Integer.compare(getHighestRank(a), getHighestRank(b));
    }

    // BOMB beats non-bomb
    if (a.type() == ComboType.BOMB) {
      return 1; // a wins
    }
    if (b.type() == ComboType.BOMB) {
      return -1; // b wins
    }

    // Non-bombs must match type and size to be comparable
    if (a.type() != b.type() || a.cards().size() != b.cards().size()) {
      throw new IllegalArgumentException(
          "Hands are not comparable: different types or sizes. a="
              + a.type()
              + "("
              + a.cards().size()
              + "), b="
              + b.type()
              + "("
              + b.cards().size()
              + ")");
    }

    // Compare same-type hands by highest rank
    // For combos like TRIPLE_WITH_SINGLE, TRIPLE_WITH_PAIR, AIRPLANE_WITH_*,
    // we compare by the rank of the primary component (triple/airplane part)
    return Integer.compare(getPrimaryRank(a), getPrimaryRank(b));
  }

  /**
   * Get the highest rank in the hand (simple max).
   *
   * @param hand the played hand
   * @return ordinal of highest rank
   */
  private int getHighestRank(PlayedHand hand) {
    return hand.cards().stream()
        .map(Card::rank)
        .max(Comparator.comparingInt(Enum::ordinal))
        .orElseThrow()
        .ordinal();
  }

  /**
   * Get the primary rank for comparison purposes.
   *
   * <p>For most hands, this is the highest rank. For combo hands with kickers (TRIPLE_WITH_SINGLE,
   * TRIPLE_WITH_PAIR, AIRPLANE_WITH_*), this extracts the rank of the main component.
   *
   * @param hand the played hand
   * @return ordinal of primary rank
   */
  private int getPrimaryRank(PlayedHand hand) {
    return switch (hand.type()) {
      case TRIPLE_WITH_SINGLE, TRIPLE_WITH_PAIR -> {
        // Find the rank that appears 3 times
        var rankCounts =
            hand.cards().stream()
                .collect(
                    java.util.stream.Collectors.groupingBy(
                        Card::rank, java.util.stream.Collectors.counting()));
        yield rankCounts.entrySet().stream()
            .filter(e -> e.getValue() == 3)
            .map(e -> e.getKey().ordinal())
            .findFirst()
            .orElseThrow();
      }
      case AIRPLANE, AIRPLANE_WITH_SINGLES, AIRPLANE_WITH_PAIRS -> {
        // Find the highest rank that appears 3 times (the airplane's highest triple)
        var rankCounts =
            hand.cards().stream()
                .collect(
                    java.util.stream.Collectors.groupingBy(
                        Card::rank, java.util.stream.Collectors.counting()));
        yield rankCounts.entrySet().stream()
            .filter(e -> e.getValue() == 3)
            .map(e -> e.getKey().ordinal())
            .max(Integer::compare)
            .orElseThrow();
      }
      default -> getHighestRank(hand); // For all other types, use highest rank
    };
  }
}
