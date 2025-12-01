package com.yourco.ddz.engine.core;

/**
 * Configuration for a Dou Dizhu game, supporting 3-12 players.
 *
 * <p>Includes preset rules for standard games and support for custom "haha funny" high-variance
 * games.
 */
public final class GameConfig {
  private final int playerCount;
  private final int deckCount;
  private final int landlordCount;
  private final int landlordExtraCards;
  private final int maxBid;
  private final boolean customDeckSize;

  private GameConfig(
      int playerCount,
      int deckCount,
      int landlordCount,
      int landlordExtraCards,
      int maxBid,
      boolean customDeckSize) {
    if (playerCount < 3 || playerCount > 12) {
      throw new IllegalArgumentException("Player count must be between 3 and 12");
    }
    if (deckCount < 1) {
      throw new IllegalArgumentException("Deck count must be at least 1");
    }
    if (landlordCount < 1 || landlordCount >= playerCount) {
      throw new IllegalArgumentException(
          "Landlord count must be at least 1 and less than player count");
    }
    if (landlordExtraCards < 0) {
      throw new IllegalArgumentException("Landlord extra cards cannot be negative");
    }
    if (maxBid < 1) {
      throw new IllegalArgumentException("Max bid must be at least 1");
    }

    this.playerCount = playerCount;
    this.deckCount = deckCount;
    this.landlordCount = landlordCount;
    this.landlordExtraCards = landlordExtraCards;
    this.maxBid = maxBid;
    this.customDeckSize = customDeckSize;
  }

  public int getPlayerCount() {
    return playerCount;
  }

  public int getDeckCount() {
    return deckCount;
  }

  public int getLandlordCount() {
    return landlordCount;
  }

  public int getLandlordExtraCards() {
    return landlordExtraCards;
  }

  public int getMaxBid() {
    return maxBid;
  }

  public boolean isCustomDeckSize() {
    return customDeckSize;
  }

  public int getTotalCards() {
    return deckCount * 54;
  }

  public int getCardsPerPlayer() {
    return (getTotalCards() - (landlordExtraCards * landlordCount)) / playerCount;
  }

  /**
   * Creates a standard game configuration with preset rules.
   *
   * <p>Preset rules: - 3 players: 1 deck, 1 landlord, max bid 3 - 4-6 players: 2 decks, 1 landlord,
   * max bid 6 - 7 players: 3 decks, 2 landlords, max bid 5 - 8 players: 3 decks, 3 landlords, max
   * bid 8 - 9 players: 4 decks, 3 landlords, max bid 9 - 10-12 players: 4 decks, 3 landlords, max
   * bid = playerCount
   *
   * @param playerCount number of players (3-12)
   * @return standard game configuration
   */
  public static GameConfig standard(int playerCount) {
    int deckCount = getStandardDeckCount(playerCount);
    int landlordCount = getStandardLandlordCount(playerCount);
    int maxBid = getStandardMaxBid(playerCount);
    int landlordExtraCards = 3; // Default: 3 cards per landlord reserved

    return new GameConfig(playerCount, deckCount, landlordCount, landlordExtraCards, maxBid, false);
  }

  /**
   * Creates a custom game configuration.
   *
   * @param playerCount number of players
   * @param deckCount number of decks to use
   * @param landlordCount number of landlords
   * @param landlordExtraCards cards reserved per landlord (bottom cards)
   * @param maxBid maximum bid value
   * @return custom game configuration
   */
  public static GameConfig custom(
      int playerCount, int deckCount, int landlordCount, int landlordExtraCards, int maxBid) {
    return new GameConfig(playerCount, deckCount, landlordCount, landlordExtraCards, maxBid, true);
  }

  /**
   * Creates a "haha funny" high-variance game with extra decks.
   *
   * @param playerCount number of players
   * @param deckMultiplier multiplier for standard deck count (e.g., 2x = double decks)
   * @return high-variance game configuration
   */
  public static GameConfig hahaFunny(int playerCount, int deckMultiplier) {
    if (deckMultiplier < 2) {
      throw new IllegalArgumentException("Deck multiplier must be at least 2 for haha funny games");
    }

    int baseDeckCount = getStandardDeckCount(playerCount);
    int deckCount = baseDeckCount * deckMultiplier;
    int landlordCount = getStandardLandlordCount(playerCount);
    int maxBid = getStandardMaxBid(playerCount);
    int landlordExtraCards = 3 * deckMultiplier; // Scale extra cards with decks

    return new GameConfig(playerCount, deckCount, landlordCount, landlordExtraCards, maxBid, true);
  }

  // ===== Helper methods for standard preset rules =====

  private static int getStandardDeckCount(int playerCount) {
    return switch (playerCount) {
      case 3 -> 1;
      case 4, 5, 6 -> 2;
      case 7, 8 -> 3;
      case 9, 10, 11, 12 -> 4;
      default -> throw new IllegalArgumentException("Player count must be 3-12");
    };
  }

  private static int getStandardLandlordCount(int playerCount) {
    return switch (playerCount) {
      case 3, 4 -> 1;
      case 5, 6, 7 -> 2;
      case 8, 9, 10, 11, 12 -> 3;
      default -> throw new IllegalArgumentException("Player count must be 3-12");
    };
  }

  private static int getStandardMaxBid(int playerCount) {
    return switch (playerCount) {
      case 3 -> 3;
      case 4, 5, 6 -> 6;
      case 7 -> 6; // Increased from 5 to compensate for distributed bombs
      case 8 -> 15; // High base to compensate for very rare bombs (18 landlord cards)
      case 9 -> 6; // Standardized from 9
      case 10 -> 7; // Moderate increase from 10
      case 11 -> 6; // Standardized from 11
      case 12 -> 6; // Standardized from 12
      default -> throw new IllegalArgumentException("Player count must be 3-12");
    };
  }

  @Override
  public String toString() {
    return String.format(
        "GameConfig{players=%d, decks=%d, landlords=%d, extraCards=%d, maxBid=%d, custom=%b}",
        playerCount, deckCount, landlordCount, landlordExtraCards, maxBid, customDeckSize);
  }
}
