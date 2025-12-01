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

    // Check if hands are bomb-type (BOMB, BOMB_WITH_SINGLES, BOMB_WITH_PAIRS)
    boolean aIsBomb = isBombType(a.type());
    boolean bIsBomb = isBombType(b.type());

    // BOMB vs BOMB: compare by bomb size first, then by rank
    if (aIsBomb && bIsBomb) {
      int aBombSize = getBombSize(a);
      int bBombSize = getBombSize(b);

      // Larger bombs beat smaller bombs
      if (aBombSize != bBombSize) {
        return Integer.compare(aBombSize, bBombSize);
      }

      // Same size bombs compared by rank
      return Integer.compare(getPrimaryRank(a), getPrimaryRank(b));
    }

    // BOMB beats non-bomb
    if (aIsBomb) {
      return 1; // a wins
    }
    if (bIsBomb) {
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
   * Check if a combo type is a bomb variant.
   *
   * @param type the combo type
   * @return true if it's a bomb type
   */
  private boolean isBombType(ComboType type) {
    return type == ComboType.BOMB
        || type == ComboType.BOMB_WITH_SINGLES
        || type == ComboType.BOMB_WITH_PAIRS;
  }

  /**
   * Get the size of the bomb itself (excluding kickers).
   *
   * @param hand the played hand (must be a bomb type)
   * @return number of cards in the bomb
   */
  private int getBombSize(PlayedHand hand) {
    return switch (hand.type()) {
      case BOMB -> hand.cards().size(); // Pure bomb
      case BOMB_WITH_SINGLES, BOMB_WITH_PAIRS -> {
        // Find the rank that appears 4+ times (the bomb)
        var rankCounts =
            hand.cards().stream()
                .collect(
                    java.util.stream.Collectors.groupingBy(
                        Card::rank, java.util.stream.Collectors.counting()));
        yield rankCounts.values().stream()
            .filter(count -> count >= 4)
            .mapToInt(Long::intValue)
            .findFirst()
            .orElseThrow();
      }
      default -> throw new IllegalArgumentException("Not a bomb type: " + hand.type());
    };
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
   * TRIPLE_WITH_PAIR, AIRPLANE_WITH_*, BOMB_WITH_*), this extracts the rank of the main component.
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
      case BOMB_WITH_SINGLES, BOMB_WITH_PAIRS -> {
        // Find the rank that appears 4+ times (the bomb)
        var rankCounts =
            hand.cards().stream()
                .collect(
                    java.util.stream.Collectors.groupingBy(
                        Card::rank, java.util.stream.Collectors.counting()));
        yield rankCounts.entrySet().stream()
            .filter(e -> e.getValue() >= 4)
            .map(e -> e.getKey().ordinal())
            .findFirst()
            .orElseThrow();
      }
      default -> getHighestRank(hand); // For all other types, use highest rank
    };
  }
}
