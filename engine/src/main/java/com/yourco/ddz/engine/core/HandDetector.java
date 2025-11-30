package com.yourco.ddz.engine.core;

import com.yourco.ddz.engine.cards.Card;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

final class HandUtil {
  private HandUtil() {}

  static Map<Card.Rank, Long> countByRank(List<Card> cards) {
    return cards.stream().collect(Collectors.groupingBy(Card::rank, Collectors.counting()));
  }

  static List<Card.Rank> sortedDistinctRanks(List<Card> cards) {
    return cards.stream()
        .map(Card::rank)
        .distinct()
        .sorted(Comparator.comparingInt(Enum::ordinal))
        .toList();
  }

  static boolean isNaturalStraightCandidate(Card.Rank r) {
    // In DDZ, straights cannot include 2 or jokers
    return r.ordinal() <= Card.Rank.ACE.ordinal(); // up to ACE
  }

  static boolean isStrictlyConsecutive(List<Card.Rank> ranks) {
    if (ranks.size() < 2) return true;
    for (int i = 1; i < ranks.size(); i++) {
      if (ranks.get(i).ordinal() != ranks.get(i - 1).ordinal() + 1) return false;
    }
    return true;
  }

  static Optional<Card.Rank> highestRank(List<Card.Rank> ranks) {
    return ranks.stream().max(Comparator.comparingInt(Enum::ordinal));
  }
}

public final class HandDetector implements PlayDetector {
  private final List<Card.HandPattern> patterns;

  public HandDetector(List<Card.HandPattern> patternsInPriority) {
    this.patterns = List.copyOf(patternsInPriority);
  }

  @Override
  public Optional<PlayedHand> detect(List<Card> cards) {
    var copy = List.copyOf(cards);
    for (var p : patterns) {
      var res = p.match(copy);
      System.out.println("res: " + res + " p: " + p);
      if (res.isPresent()) return res;
    }
    return Optional.empty();
  }

  public static HandDetector defaultDdz() {
    return new HandDetector(
        List.of(
            // Patterns ordered from most specific to least specific
            new RocketPattern(), // Must be checked first - both jokers
            new BombWithPairsPattern(), // Bomb + 2 pairs (more specific than plain bomb)
            new BombWithSinglesPattern(), // Bomb + 2 singles (more specific than plain bomb)
            new BombPattern(), // Plain bomb (4+ of a kind)
            new AirplaneWithPairsPattern(), // Most complex airplane variant
            new AirplaneWithSinglesPattern(), // Airplane with singles
            new AirplanePattern(), // Plain airplane (consecutive triples)
            new TripleWithPairPattern(), // 3 + pair
            new TripleWithSinglePattern(), // 3 + single
            new ConsecutivePairsPattern(), // Consecutive pairs
            new StraightPattern(), // Sequence of 5+ singles
            new TriplePattern(), // Plain triple
            new PairPattern(), // Pair
            new SinglePattern() // Single card - least specific
            ));
  }
}

// ROCKET: small + big joker (exactly 2 cards)
final class RocketPattern implements Card.HandPattern {
  @Override
  public Optional<PlayedHand> match(List<Card> cards) {
    if (cards.size() != 2) return Optional.empty();
    var ranks = cards.stream().map(Card::rank).collect(Collectors.toSet());
    if (ranks.contains(Card.Rank.LITTLE_JOKER) && ranks.contains(Card.Rank.BIG_JOKER)) {
      return Optional.of(new PlayedHand(ComboType.ROCKET, cards));
    }
    return Optional.empty();
  }
}

// PAIR (2 cards)
final class PairPattern implements Card.HandPattern {
  @Override
  public Optional<PlayedHand> match(List<Card> cards) {
    if (cards.size() != 2) return Optional.empty();
    return cards.get(0).rank() == cards.get(1).rank()
            && cards.get(0).rank() != Card.Rank.LITTLE_JOKER
            && cards.get(0).rank() != Card.Rank.BIG_JOKER
        ? Optional.of(new PlayedHand(ComboType.PAIR, cards))
        : Optional.empty();
  }
}

// SINGLE (1 card)
final class SinglePattern implements Card.HandPattern {
  @Override
  public Optional<PlayedHand> match(List<Card> cards) {
    if (cards.size() != 1) return Optional.empty();
    return Optional.of(new PlayedHand(ComboType.SINGLE, cards));
  }
}

// BOMB: 4+ cards of the same rank (multi-deck games support 5, 6, 7, 8+ card bombs)
final class BombPattern implements Card.HandPattern {
  @Override
  public Optional<PlayedHand> match(List<Card> cards) {
    if (cards.size() < 4) return Optional.empty();
    var rankCounts = HandUtil.countByRank(cards);
    if (rankCounts.size() == 1) {
      var rank = rankCounts.keySet().iterator().next();
      // Jokers cannot form a bomb (they form a rocket)
      if (rank != Card.Rank.LITTLE_JOKER && rank != Card.Rank.BIG_JOKER) {
        return Optional.of(new PlayedHand(ComboType.BOMB, cards));
      }
    }
    return Optional.empty();
  }
}

// BOMB_WITH_SINGLES: Bomb (4+ of a kind) + 2 singles
// Example: 7-7-7-7-3-5 (4-bomb + 2 singles) or 7-7-7-7-7-3-5 (5-bomb + 2 singles)
// Note: Does NOT count for bomb multiplier
final class BombWithSinglesPattern implements Card.HandPattern {
  @Override
  public Optional<PlayedHand> match(List<Card> cards) {
    if (cards.size() < 6) return Optional.empty(); // Minimum: 4-bomb + 2 singles
    var rankCounts = HandUtil.countByRank(cards);

    // Need at least 3 ranks: bomb rank + 2 different singles
    if (rankCounts.size() < 3) return Optional.empty();

    // Find bomb (4+ of same rank)
    var bombRank =
        rankCounts.entrySet().stream()
            .filter(e -> e.getValue() >= 4)
            .map(Map.Entry::getKey)
            .findFirst();

    if (bombRank.isEmpty()) return Optional.empty();

    // Check that remaining cards are 2 singles (each appears once)
    long bombCount = rankCounts.get(bombRank.get());
    long expectedTotalCards = bombCount + 2; // bomb + 2 singles

    if (cards.size() != expectedTotalCards) return Optional.empty();

    // Count singles (ranks that appear exactly once)
    long singleCount =
        rankCounts.entrySet().stream()
            .filter(e -> !e.getKey().equals(bombRank.get()) && e.getValue() == 1)
            .count();

    if (singleCount == 2) {
      return Optional.of(new PlayedHand(ComboType.BOMB_WITH_SINGLES, cards));
    }

    return Optional.empty();
  }
}

// BOMB_WITH_PAIRS: Bomb (4+ of a kind) + 2 pairs
// Example: 7-7-7-7-3-3-4-4 (4-bomb + 2 pairs) or 7-7-7-7-7-3-3-4-4 (5-bomb + 2 pairs)
// Note: Does NOT count for bomb multiplier
final class BombWithPairsPattern implements Card.HandPattern {
  @Override
  public Optional<PlayedHand> match(List<Card> cards) {
    if (cards.size() < 8) return Optional.empty(); // Minimum: 4-bomb + 2 pairs (4+2+2)
    var rankCounts = HandUtil.countByRank(cards);

    // Need at least 3 ranks: bomb rank + 2 different pairs
    if (rankCounts.size() < 3) return Optional.empty();

    // Find bomb (4+ of same rank)
    var bombRank =
        rankCounts.entrySet().stream()
            .filter(e -> e.getValue() >= 4)
            .map(Map.Entry::getKey)
            .findFirst();

    if (bombRank.isEmpty()) return Optional.empty();

    // Check that remaining cards are 2 pairs (each appears twice)
    long bombCount = rankCounts.get(bombRank.get());
    long expectedTotalCards = bombCount + 4; // bomb + 2 pairs (2+2)

    if (cards.size() != expectedTotalCards) return Optional.empty();

    // Count pairs (ranks that appear exactly twice)
    long pairCount =
        rankCounts.entrySet().stream()
            .filter(e -> !e.getKey().equals(bombRank.get()) && e.getValue() == 2)
            .count();

    if (pairCount == 2) {
      return Optional.of(new PlayedHand(ComboType.BOMB_WITH_PAIRS, cards));
    }

    return Optional.empty();
  }
}

// TRIPLE: 3 cards of the same rank
final class TriplePattern implements Card.HandPattern {
  @Override
  public Optional<PlayedHand> match(List<Card> cards) {
    if (cards.size() != 3) return Optional.empty();
    var rankCounts = HandUtil.countByRank(cards);
    if (rankCounts.size() == 1) {
      var rank = rankCounts.keySet().iterator().next();
      // Jokers cannot form a triple
      if (rank != Card.Rank.LITTLE_JOKER && rank != Card.Rank.BIG_JOKER) {
        return Optional.of(new PlayedHand(ComboType.TRIPLE, cards));
      }
    }
    return Optional.empty();
  }
}

// TRIPLE_WITH_SINGLE: 3 of a kind + 1 single (4 cards total)
final class TripleWithSinglePattern implements Card.HandPattern {
  @Override
  public Optional<PlayedHand> match(List<Card> cards) {
    if (cards.size() != 4) return Optional.empty();
    var rankCounts = HandUtil.countByRank(cards);
    if (rankCounts.size() == 2) {
      // One rank appears 3 times, one appears 1 time
      var tripleRank =
          rankCounts.entrySet().stream()
              .filter(e -> e.getValue() == 3)
              .map(Map.Entry::getKey)
              .findFirst();
      var singleRank =
          rankCounts.entrySet().stream()
              .filter(e -> e.getValue() == 1)
              .map(Map.Entry::getKey)
              .findFirst();
      if (tripleRank.isPresent() && singleRank.isPresent()) {
        return Optional.of(new PlayedHand(ComboType.TRIPLE_WITH_SINGLE, cards));
      }
    }
    return Optional.empty();
  }
}

// TRIPLE_WITH_PAIR: 3 of a kind + 1 pair (5 cards total)
final class TripleWithPairPattern implements Card.HandPattern {
  @Override
  public Optional<PlayedHand> match(List<Card> cards) {
    if (cards.size() != 5) return Optional.empty();
    var rankCounts = HandUtil.countByRank(cards);
    if (rankCounts.size() == 2) {
      // One rank appears 3 times, one appears 2 times
      var tripleRank =
          rankCounts.entrySet().stream()
              .filter(e -> e.getValue() == 3)
              .map(Map.Entry::getKey)
              .findFirst();
      var pairRank =
          rankCounts.entrySet().stream()
              .filter(e -> e.getValue() == 2)
              .map(Map.Entry::getKey)
              .findFirst();
      if (tripleRank.isPresent() && pairRank.isPresent()) {
        return Optional.of(new PlayedHand(ComboType.TRIPLE_WITH_PAIR, cards));
      }
    }
    return Optional.empty();
  }
}

// SEQUENCE (Straight): 5+ consecutive cards (singles)
final class StraightPattern implements Card.HandPattern {
  @Override
  public Optional<PlayedHand> match(List<Card> cards) {
    if (cards.size() < 5) return Optional.empty();
    var rankCounts = HandUtil.countByRank(cards);
    // Each rank must appear exactly once
    if (rankCounts.size() != cards.size()) return Optional.empty();
    var ranks = HandUtil.sortedDistinctRanks(cards);

    // Check for jokers - never allowed in straights
    if (ranks.contains(Card.Rank.LITTLE_JOKER) || ranks.contains(Card.Rank.BIG_JOKER)) {
      return Optional.empty();
    }

    // Special case: Allow A-2 at the end (e.g., J-Q-K-A-2)
    boolean hasTwo = ranks.contains(Card.Rank.TWO);
    boolean hasAce = ranks.contains(Card.Rank.ACE);

    if (hasTwo) {
      // If 2 is present, ACE must also be present (for K-A-2 pattern)
      if (!hasAce) {
        return Optional.empty();
      }
      // 2 must be at the highest position (last in sorted order)
      if (ranks.get(ranks.size() - 1) != Card.Rank.TWO) {
        return Optional.empty();
      }
      // Check: everything before 2 must be consecutive and end with ACE
      var ranksWithoutTwo = ranks.subList(0, ranks.size() - 1);
      if (ranksWithoutTwo.get(ranksWithoutTwo.size() - 1) != Card.Rank.ACE) {
        return Optional.empty();
      }
      // Check if everything before 2 is consecutive
      if (HandUtil.isStrictlyConsecutive(ranksWithoutTwo)) {
        return Optional.of(new PlayedHand(ComboType.SEQUENCE, cards));
      }
    } else {
      // Normal case: no 2, all ranks must be valid (3-A)
      if (!ranks.stream().allMatch(HandUtil::isNaturalStraightCandidate)) {
        return Optional.empty();
      }
      // Check if consecutive
      if (HandUtil.isStrictlyConsecutive(ranks)) {
        return Optional.of(new PlayedHand(ComboType.SEQUENCE, cards));
      }
    }

    return Optional.empty();
  }
}

// PAIR_SEQUENCE (Consecutive Pairs): 3+ consecutive pairs (6+ cards)
final class ConsecutivePairsPattern implements Card.HandPattern {
  @Override
  public Optional<PlayedHand> match(List<Card> cards) {
    if (cards.size() < 6 || cards.size() % 2 != 0) return Optional.empty();
    var rankCounts = HandUtil.countByRank(cards);
    // Each rank must appear exactly twice
    if (!rankCounts.values().stream().allMatch(count -> count == 2)) {
      return Optional.empty();
    }
    var ranks = HandUtil.sortedDistinctRanks(cards);
    // Need at least 3 pairs
    if (ranks.size() < 3) return Optional.empty();

    // Check for jokers - never allowed
    if (ranks.contains(Card.Rank.LITTLE_JOKER) || ranks.contains(Card.Rank.BIG_JOKER)) {
      return Optional.empty();
    }

    // Special case: Allow A-2 at the end
    boolean hasTwo = ranks.contains(Card.Rank.TWO);
    boolean hasAce = ranks.contains(Card.Rank.ACE);

    if (hasTwo) {
      // If 2 is present, ACE must also be present
      if (!hasAce) {
        return Optional.empty();
      }
      // 2 must be at the highest position
      if (ranks.get(ranks.size() - 1) != Card.Rank.TWO) {
        return Optional.empty();
      }
      // Everything before 2 must be consecutive and end with ACE
      var ranksWithoutTwo = ranks.subList(0, ranks.size() - 1);
      if (ranksWithoutTwo.get(ranksWithoutTwo.size() - 1) != Card.Rank.ACE) {
        return Optional.empty();
      }
      if (HandUtil.isStrictlyConsecutive(ranksWithoutTwo)) {
        return Optional.of(new PlayedHand(ComboType.PAIR_SEQUENCE, cards));
      }
    } else {
      // Normal case: all ranks must be valid (3-A)
      if (!ranks.stream().allMatch(HandUtil::isNaturalStraightCandidate)) {
        return Optional.empty();
      }
      // Check if consecutive
      if (HandUtil.isStrictlyConsecutive(ranks)) {
        return Optional.of(new PlayedHand(ComboType.PAIR_SEQUENCE, cards));
      }
    }

    return Optional.empty();
  }
}

// AIRPLANE: 2+ consecutive triples (6+ cards, must be multiple of 3)
final class AirplanePattern implements Card.HandPattern {
  @Override
  public Optional<PlayedHand> match(List<Card> cards) {
    if (cards.size() < 6 || cards.size() % 3 != 0) return Optional.empty();
    var rankCounts = HandUtil.countByRank(cards);
    // Each rank must appear exactly 3 times
    if (!rankCounts.values().stream().allMatch(count -> count == 3)) {
      return Optional.empty();
    }
    var ranks = HandUtil.sortedDistinctRanks(cards);
    // Need at least 2 triples
    if (ranks.size() < 2) return Optional.empty();

    // Check for jokers - never allowed
    if (ranks.contains(Card.Rank.LITTLE_JOKER) || ranks.contains(Card.Rank.BIG_JOKER)) {
      return Optional.empty();
    }

    // Special case: Allow A-2 at the end
    boolean hasTwo = ranks.contains(Card.Rank.TWO);
    boolean hasAce = ranks.contains(Card.Rank.ACE);

    if (hasTwo) {
      if (!hasAce) {
        return Optional.empty();
      }
      if (ranks.get(ranks.size() - 1) != Card.Rank.TWO) {
        return Optional.empty();
      }
      var ranksWithoutTwo = ranks.subList(0, ranks.size() - 1);
      if (ranksWithoutTwo.get(ranksWithoutTwo.size() - 1) != Card.Rank.ACE) {
        return Optional.empty();
      }
      if (HandUtil.isStrictlyConsecutive(ranksWithoutTwo)) {
        return Optional.of(new PlayedHand(ComboType.AIRPLANE, cards));
      }
    } else {
      // Normal case
      if (!ranks.stream().allMatch(HandUtil::isNaturalStraightCandidate)) {
        return Optional.empty();
      }
      if (HandUtil.isStrictlyConsecutive(ranks)) {
        return Optional.of(new PlayedHand(ComboType.AIRPLANE, cards));
      }
    }

    return Optional.empty();
  }
}

// AIRPLANE_WITH_SINGLES: 2+ consecutive triples + same number of singles
final class AirplaneWithSinglesPattern implements Card.HandPattern {
  @Override
  public Optional<PlayedHand> match(List<Card> cards) {
    if (cards.size() < 8) return Optional.empty(); // Minimum: 2 triples + 2 singles
    var rankCounts = HandUtil.countByRank(cards);

    // Separate triples from singles
    var tripleRanks =
        rankCounts.entrySet().stream()
            .filter(e -> e.getValue() == 3)
            .map(Map.Entry::getKey)
            .sorted(Comparator.comparingInt(Enum::ordinal))
            .toList();

    // Count remaining cards (should all be singles)
    long singleCount = rankCounts.entrySet().stream().filter(e -> e.getValue() == 1).count();

    // Need at least 2 consecutive triples
    if (tripleRanks.size() < 2) return Optional.empty();

    // Number of singles must equal number of triples
    if (singleCount != tripleRanks.size()) return Optional.empty();

    // Check for jokers in triples
    if (tripleRanks.contains(Card.Rank.LITTLE_JOKER) || tripleRanks.contains(Card.Rank.BIG_JOKER)) {
      return Optional.empty();
    }

    // Special case: Allow A-2 at the end of triple sequence
    boolean hasTwo = tripleRanks.contains(Card.Rank.TWO);
    boolean hasAce = tripleRanks.contains(Card.Rank.ACE);

    if (hasTwo) {
      if (!hasAce) {
        return Optional.empty();
      }
      if (tripleRanks.get(tripleRanks.size() - 1) != Card.Rank.TWO) {
        return Optional.empty();
      }
      var ranksWithoutTwo = tripleRanks.subList(0, tripleRanks.size() - 1);
      if (ranksWithoutTwo.get(ranksWithoutTwo.size() - 1) != Card.Rank.ACE) {
        return Optional.empty();
      }
      if (HandUtil.isStrictlyConsecutive(ranksWithoutTwo)) {
        return Optional.of(new PlayedHand(ComboType.AIRPLANE_WITH_SINGLES, cards));
      }
    } else {
      // Normal case
      if (!tripleRanks.stream().allMatch(HandUtil::isNaturalStraightCandidate)) {
        return Optional.empty();
      }
      if (HandUtil.isStrictlyConsecutive(tripleRanks)) {
        return Optional.of(new PlayedHand(ComboType.AIRPLANE_WITH_SINGLES, cards));
      }
    }

    return Optional.empty();
  }
}

// AIRPLANE_WITH_PAIRS: 2+ consecutive triples + same number of pairs
final class AirplaneWithPairsPattern implements Card.HandPattern {
  @Override
  public Optional<PlayedHand> match(List<Card> cards) {
    if (cards.size() < 10) return Optional.empty(); // Minimum: 2 triples + 2 pairs
    var rankCounts = HandUtil.countByRank(cards);

    // Separate triples from pairs
    var tripleRanks =
        rankCounts.entrySet().stream()
            .filter(e -> e.getValue() == 3)
            .map(Map.Entry::getKey)
            .sorted(Comparator.comparingInt(Enum::ordinal))
            .toList();

    // Count pairs
    long pairCount = rankCounts.entrySet().stream().filter(e -> e.getValue() == 2).count();

    // Need at least 2 consecutive triples
    if (tripleRanks.size() < 2) return Optional.empty();

    // Number of pairs must equal number of triples
    if (pairCount != tripleRanks.size()) return Optional.empty();

    // Check for jokers in triples
    if (tripleRanks.contains(Card.Rank.LITTLE_JOKER) || tripleRanks.contains(Card.Rank.BIG_JOKER)) {
      return Optional.empty();
    }

    // Special case: Allow A-2 at the end of triple sequence
    boolean hasTwo = tripleRanks.contains(Card.Rank.TWO);
    boolean hasAce = tripleRanks.contains(Card.Rank.ACE);

    if (hasTwo) {
      if (!hasAce) {
        return Optional.empty();
      }
      if (tripleRanks.get(tripleRanks.size() - 1) != Card.Rank.TWO) {
        return Optional.empty();
      }
      var ranksWithoutTwo = tripleRanks.subList(0, tripleRanks.size() - 1);
      if (ranksWithoutTwo.get(ranksWithoutTwo.size() - 1) != Card.Rank.ACE) {
        return Optional.empty();
      }
      if (HandUtil.isStrictlyConsecutive(ranksWithoutTwo)) {
        return Optional.of(new PlayedHand(ComboType.AIRPLANE_WITH_PAIRS, cards));
      }
    } else {
      // Normal case
      if (!tripleRanks.stream().allMatch(HandUtil::isNaturalStraightCandidate)) {
        return Optional.empty();
      }
      if (HandUtil.isStrictlyConsecutive(tripleRanks)) {
        return Optional.of(new PlayedHand(ComboType.AIRPLANE_WITH_PAIRS, cards));
      }
    }

    return Optional.empty();
  }
}
