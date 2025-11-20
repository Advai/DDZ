package com.yourco.ddz.engine.core;

import static com.yourco.ddz.engine.core.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for play comparison logic. */
class SimplePlayComparatorTest {

  private PlayComparator comparator;
  private PlayDetector detector;

  @BeforeEach
  void setUp() {
    comparator = new SimplePlayComparator();
    detector = HandDetector.defaultDdz();
  }

  private PlayedHand detectHand(String... notations) {
    return detector.detect(cards(notations)).orElseThrow();
  }

  // ============= ROCKET TESTS =============

  @Test
  void testRocketBeatsBomb() {
    PlayedHand rocket = detectHand("LJ", "BJ");
    PlayedHand bomb = detectHand("3H", "3D", "3S", "3C");

    int result = comparator.compare(rocket, bomb);
    assertTrue(result > 0, "Rocket should beat bomb");
  }

  @Test
  void testRocketBeatsTriple() {
    PlayedHand rocket = detectHand("LJ", "BJ");
    PlayedHand triple = detectHand("AH", "AD", "AS");

    int result = comparator.compare(rocket, triple);
    assertTrue(result > 0, "Rocket should beat triple");
  }

  @Test
  void testRocketBeatsPair() {
    PlayedHand rocket = detectHand("LJ", "BJ");
    PlayedHand pair = detectHand("2H", "2D");

    int result = comparator.compare(rocket, pair);
    assertTrue(result > 0, "Rocket should beat pair");
  }

  // ============= BOMB TESTS =============

  @Test
  void testBombBeatsNonBomb() {
    PlayedHand bomb = detectHand("3H", "3D", "3S", "3C");
    PlayedHand triple = detectHand("AH", "AD", "AS");

    int result = comparator.compare(bomb, triple);
    assertTrue(result > 0, "Bomb should beat non-bomb");
  }

  @Test
  void testBombVsBombHigherRankWins() {
    PlayedHand bomb9 = detectHand("9H", "9D", "9S", "9C");
    PlayedHand bomb3 = detectHand("3H", "3D", "3S", "3C");

    int result = comparator.compare(bomb9, bomb3);
    assertTrue(result > 0, "Higher bomb should win");
  }

  @Test
  void testBombVsBombLowerRankLoses() {
    PlayedHand bomb3 = detectHand("3H", "3D", "3S", "3C");
    PlayedHand bombK = detectHand("KH", "KD", "KS", "KC");

    int result = comparator.compare(bomb3, bombK);
    assertTrue(result < 0, "Lower bomb should lose");
  }

  @Test
  void testBombLosesToRocket() {
    PlayedHand bomb = detectHand("AH", "AD", "AS", "AC");
    PlayedHand rocket = detectHand("LJ", "BJ");

    int result = comparator.compare(bomb, rocket);
    assertTrue(result < 0, "Bomb should lose to rocket");
  }

  // ============= SAME TYPE COMPARISON =============

  @Test
  void testTripleVsTripleHigherRankWins() {
    PlayedHand triple7 = detectHand("7H", "7D", "7S");
    PlayedHand triple4 = detectHand("4H", "4D", "4S");

    int result = comparator.compare(triple7, triple4);
    assertTrue(result > 0, "Higher triple should win");
  }

  @Test
  void testPairVsPairHigherRankWins() {
    PlayedHand pairQ = detectHand("QH", "QD");
    PlayedHand pair5 = detectHand("5H", "5D");

    int result = comparator.compare(pairQ, pair5);
    assertTrue(result > 0, "Higher pair should win");
  }

  @Test
  void testSingleVsSingleHigherRankWins() {
    PlayedHand singleA = detectHand("AH");
    PlayedHand single3 = detectHand("3D");

    int result = comparator.compare(singleA, single3);
    assertTrue(result > 0, "Higher single should win");
  }

  @Test
  void testStraightVsStraightHigherRankWins() {
    PlayedHand straight1 = detectHand("5H", "6D", "7S", "8C", "9H");
    PlayedHand straight2 = detectHand("3H", "4D", "5S", "6C", "7D");

    int result = comparator.compare(straight1, straight2);
    assertTrue(result > 0, "Higher straight should win");
  }

  // ============= TYPE MISMATCH TESTS =============

  @Test
  void testTypeMismatchThrowsException() {
    PlayedHand triple = detectHand("5H", "5D", "5S");
    PlayedHand pair = detectHand("AH", "AD");

    assertThrows(
        IllegalArgumentException.class,
        () -> comparator.compare(triple, pair),
        "Comparing different types should throw");
  }

  @Test
  void testSizeMismatchThrowsException() {
    PlayedHand straight5 = detectHand("3H", "4D", "5S", "6C", "7H");
    PlayedHand straight6 = detectHand("4H", "5D", "6S", "7C", "8H", "9D");

    assertThrows(
        IllegalArgumentException.class,
        () -> comparator.compare(straight5, straight6),
        "Different sized straights should throw");
  }

  // ============= PRIMARY RANK EXTRACTION TESTS =============

  @Test
  void testTripleWithSingleComparison() {
    PlayedHand hand1 = detectHand("7H", "7D", "7S", "3C");
    PlayedHand hand2 = detectHand("5H", "5D", "5S", "AC");

    int result = comparator.compare(hand1, hand2);
    assertTrue(result > 0, "777+3 should beat 555+A (primary rank is triple)");
  }

  @Test
  void testAirplaneComparison() {
    PlayedHand airplane1 = detectHand("5H", "5D", "5S", "6H", "6D", "6S");
    PlayedHand airplane2 = detectHand("3H", "3D", "3S", "4H", "4D", "4S");

    int result = comparator.compare(airplane1, airplane2);
    assertTrue(result > 0, "555-666 should beat 333-444 (highest triple determines)");
  }

  // ============= EDGE CASES =============

  @Test
  void testTwosAreHighest() {
    PlayedHand pair2 = detectHand("2H", "2D");
    PlayedHand pairA = detectHand("AH", "AD");

    int result = comparator.compare(pair2, pairA);
    assertTrue(result > 0, "Pair of 2s should beat pair of Aces");
  }

  @Test
  void testEqualHandsReturnZero() {
    PlayedHand triple1 = detectHand("5H", "5D", "5S");
    PlayedHand triple2 = detectHand("5C", "5D", "5S");

    int result = comparator.compare(triple1, triple2);
    assertEquals(0, result, "Equal triples should return 0");
  }
}
