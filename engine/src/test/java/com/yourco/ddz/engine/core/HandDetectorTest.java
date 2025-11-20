package com.yourco.ddz.engine.core;

import static com.yourco.ddz.engine.core.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for hand detection patterns. */
class HandDetectorTest {

  private HandDetector detector;

  @BeforeEach
  void setUp() {
    detector = HandDetector.defaultDdz();
  }

  // ============= SINGLE TESTS =============

  @Test
  void testSingleCard() {
    var result = detector.detect(cards("3H"));
    assertTrue(result.isPresent());
    assertEquals(ComboType.SINGLE, result.get().type());
  }

  @Test
  void testEmptyListInvalid() {
    var result = detector.detect(List.of());
    assertTrue(result.isEmpty());
  }

  // ============= PAIR TESTS =============

  @Test
  void testValidPair() {
    var result = detector.detect(cards("3H", "3D"));
    assertTrue(result.isPresent());
    assertEquals(ComboType.PAIR, result.get().type());
  }

  @Test
  void testPairMismatchedRanks() {
    var result = detector.detect(cards("3H", "4D"));
    // Two cards with different ranks - invalid combo
    // In DDZ, this would be two separate singles, not a valid single play
    assertTrue(result.isEmpty());
  }

  // ============= TRIPLE TESTS =============

  @Test
  void testValidTriple() {
    var result = detector.detect(cards("5H", "5D", "5S"));
    assertTrue(result.isPresent());
    assertEquals(ComboType.TRIPLE, result.get().type());
  }

  @Test
  void testTripleWrongCount() {
    var result = detector.detect(cards("5H", "5D"));
    assertTrue(result.isPresent());
    // Two cards - should be detected as pair, not triple
    assertEquals(ComboType.PAIR, result.get().type());
  }

  // ============= BOMB TESTS =============

  @Test
  void testValidBomb() {
    var result = detector.detect(cards("7H", "7D", "7S", "7C"));
    assertTrue(result.isPresent());
    assertEquals(ComboType.BOMB, result.get().type());
  }

  @Test
  void testBombWrongCount() {
    var result = detector.detect(cards("7H", "7D", "7S"));
    assertTrue(result.isPresent());
    // Three 7s - should be triple, not bomb
    assertEquals(ComboType.TRIPLE, result.get().type());
  }

  // ============= ROCKET TESTS =============

  @Test
  void testValidRocket() {
    var result = detector.detect(cards("LJ", "BJ"));
    assertTrue(result.isPresent());
    assertEquals(ComboType.ROCKET, result.get().type());
  }

  @Test
  void testRocketOneJokerOnly() {
    var result = detector.detect(cards("LJ"));
    assertTrue(result.isPresent());
    // Single joker - should be single, not rocket
    assertEquals(ComboType.SINGLE, result.get().type());
  }

  @Test
  void testRocketWithExtraCards() {
    var result = detector.detect(cards("LJ", "BJ", "3H"));
    assertTrue(result.isEmpty()); // Invalid combo
  }

  // ============= STRAIGHT TESTS =============

  @Test
  void testValidStraight5Cards() {
    var result = detector.detect(cards("3H", "4D", "5S", "6C", "7H"));
    assertTrue(result.isPresent());
    assertEquals(ComboType.SEQUENCE, result.get().type());
  }

  @Test
  void testValidStraight7Cards() {
    var result = detector.detect(cards("3H", "4D", "5S", "6C", "7H", "8D", "9S"));
    assertTrue(result.isPresent());
    assertEquals(ComboType.SEQUENCE, result.get().type());
  }

  @Test
  void testStraightTooShort() {
    var result = detector.detect(cards("3H", "4D", "5S", "6C"));
    // 4 consecutive cards - too short for straight (need 5+)
    assertTrue(result.isEmpty());
  }

  @Test
  void testStraightWithTwoAfterAceValid() {
    var result = detector.detect(cards("TH", "JD", "QS", "KC", "AH", "2D"));
    // T-J-Q-K-A-2 is valid (2 can follow Ace)
    assertTrue(result.isPresent());
    assertEquals(ComboType.SEQUENCE, result.get().type());
  }

  @Test
  void testStraightWithTwoWithoutAceInvalid() {
    var result = detector.detect(cards("3H", "4D", "5S", "6C", "7H", "2D"));
    // 2 cannot be in straights without Ace - invalid combo
    assertTrue(result.isEmpty());
  }

  @Test
  void testStraightStartingWithTwoInvalid() {
    var result = detector.detect(cards("2H", "3D", "4S", "5C", "6H"));
    // 2-3-4-5-6 is invalid (2 cannot start a straight)
    assertTrue(result.isEmpty());
  }

  @Test
  void testStraightWithJokersInvalid() {
    var result = detector.detect(cards("3H", "4D", "5S", "6C", "LJ"));
    assertTrue(result.isEmpty()); // Invalid
  }

  @Test
  void testStraightNonConsecutive() {
    var result = detector.detect(cards("3H", "4D", "5S", "7C", "8H"));
    // Missing 6, not a straight - invalid combo
    assertTrue(result.isEmpty());
  }

  @Test
  void testStraightMaxLength() {
    var result =
        detector.detect(
            cards("3H", "4D", "5S", "6C", "7H", "8D", "9S", "TC", "JH", "QD", "KS", "AH"));
    assertTrue(result.isPresent());
    assertEquals(ComboType.SEQUENCE, result.get().type());
  }

  // ============= CONSECUTIVE PAIRS TESTS =============

  @Test
  void testValidConsecutivePairs3Pairs() {
    var result = detector.detect(cards("3H", "3D", "4S", "4C", "5H", "5D"));
    assertTrue(result.isPresent());
    assertEquals(ComboType.PAIR_SEQUENCE, result.get().type());
  }

  @Test
  void testValidConsecutivePairs5Pairs() {
    var result = detector.detect(cards("3H", "3D", "4S", "4C", "5H", "5D", "6S", "6C", "7H", "7D"));
    assertTrue(result.isPresent());
    assertEquals(ComboType.PAIR_SEQUENCE, result.get().type());
  }

  @Test
  void testConsecutivePairsTooShort() {
    var result = detector.detect(cards("3H", "3D", "4S", "4C"));
    // Only 2 pairs, need at least 3 - invalid combo
    assertTrue(result.isEmpty());
  }

  @Test
  void testConsecutivePairsNonConsecutive() {
    var result = detector.detect(cards("3H", "3D", "4S", "4C", "6H", "6D"));
    // Missing 5s - not consecutive, invalid combo
    assertTrue(result.isEmpty());
  }

  // ============= AIRPLANE TESTS =============

  @Test
  void testValidAirplane2Triples() {
    var result = detector.detect(cards("3H", "3D", "3S", "4H", "4D", "4S"));
    assertTrue(result.isPresent());
    assertEquals(ComboType.AIRPLANE, result.get().type());
  }

  @Test
  void testValidAirplane3Triples() {
    var result = detector.detect(cards("5H", "5D", "5S", "6H", "6D", "6S", "7H", "7D", "7S"));
    assertTrue(result.isPresent());
    assertEquals(ComboType.AIRPLANE, result.get().type());
  }

  @Test
  void testAirplaneNonConsecutive() {
    var result = detector.detect(cards("3H", "3D", "3S", "5H", "5D", "5S"));
    // Missing 4s - not consecutive, invalid combo
    assertTrue(result.isEmpty());
  }

  // ============= AIRPLANE WITH SINGLES TESTS =============

  @Test
  void testValidAirplaneWithSingles() {
    var result = detector.detect(cards("3H", "3D", "3S", "4H", "4D", "4S", "5C", "6C"));
    assertTrue(result.isPresent());
    assertEquals(ComboType.AIRPLANE_WITH_SINGLES, result.get().type());
  }

  @Test
  void testAirplaneWithSinglesWrongKickerCount() {
    var result = detector.detect(cards("3H", "3D", "3S", "4H", "4D", "4S", "5C"));
    // Need 2 kickers for 2 triples, only have 1 - invalid combo
    assertTrue(result.isEmpty());
  }

  // ============= AIRPLANE WITH PAIRS TESTS =============

  @Test
  void testValidAirplaneWithPairs() {
    var result = detector.detect(cards("3H", "3D", "3S", "4H", "4D", "4S", "5C", "5D", "6C", "6D"));
    assertTrue(result.isPresent());
    assertEquals(ComboType.AIRPLANE_WITH_PAIRS, result.get().type());
  }

  @Test
  void testAirplaneWithPairsWrongKickerCount() {
    var result = detector.detect(cards("3H", "3D", "3S", "4H", "4D", "4S", "5C", "5D"));
    // Need 2 pairs for 2 triples, only have 1 pair - invalid combo
    assertTrue(result.isEmpty());
  }

  // ============= TRIPLE WITH SINGLE TESTS =============

  @Test
  void testValidTripleWithSingle() {
    var result = detector.detect(cards("5H", "5D", "5S", "3C"));
    assertTrue(result.isPresent());
    assertEquals(ComboType.TRIPLE_WITH_SINGLE, result.get().type());
  }

  @Test
  void testTripleWithSingleWrongKickerCount() {
    var result = detector.detect(cards("5H", "5D", "5S", "3C", "4C"));
    // Too many kickers - invalid combo
    assertTrue(result.isEmpty());
  }

  // ============= TRIPLE WITH PAIR TESTS =============

  @Test
  void testValidTripleWithPair() {
    var result = detector.detect(cards("5H", "5D", "5S", "3C", "3D"));
    assertTrue(result.isPresent());
    assertEquals(ComboType.TRIPLE_WITH_PAIR, result.get().type());
  }

  @Test
  void testTripleWithPairWrongKickerCount() {
    var result = detector.detect(cards("5H", "5D", "5S", "3C"));
    assertTrue(result.isPresent());
    // Need pair, only have single - should match TRIPLE_WITH_SINGLE instead
    assertEquals(ComboType.TRIPLE_WITH_SINGLE, result.get().type());
  }

  // ============= PATTERN PRIORITY TESTS =============

  @Test
  void testPatternPriorityAirplaneOverSingles() {
    // 333-444 should be detected as airplane, not 6 singles
    var result = detector.detect(cards("3H", "3D", "3S", "4H", "4D", "4S"));
    assertTrue(result.isPresent());
    assertEquals(ComboType.AIRPLANE, result.get().type());
  }

  @Test
  void testPatternPriorityBombOverTriple() {
    // 5555 should be bomb, not triple with single
    var result = detector.detect(cards("5H", "5D", "5S", "5C"));
    assertTrue(result.isPresent());
    assertEquals(ComboType.BOMB, result.get().type());
  }
}
