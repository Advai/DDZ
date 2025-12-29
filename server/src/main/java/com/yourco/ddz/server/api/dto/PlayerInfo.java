package com.yourco.ddz.server.api.dto;

import com.yourco.ddz.engine.core.GameState;
import java.util.List;
import java.util.UUID;

public record PlayerInfo(
    String id,
    String name,
    int cardCount,
    boolean isLandlord,
    boolean isConnected,
    int score,
    int bid,
    Integer seatPosition,
    List<CardDto> visibleCards) {
  public static PlayerInfo from(GameState state, UUID playerId) {
    return from(state, playerId, null);
  }

  public static PlayerInfo from(GameState state, UUID playerId, Integer seatPosition) {
    // Show only the bottom cards that are still in the landlord's hand
    List<CardDto> visibleCards = null;
    if (state.isLandlord(playerId)) {
      var currentHand = state.handOf(playerId);
      visibleCards =
          state.getLandlordBottomCards(playerId).stream()
              .filter(currentHand::contains) // Only show cards still in hand
              .map(CardDto::from)
              .toList();
    }

    return new PlayerInfo(
        playerId.toString(),
        state.getPlayerName(playerId),
        state.handOf(playerId).size(),
        state.isLandlord(playerId),
        state.isPlayerConnected(playerId),
        state.getScores().getOrDefault(playerId, 0),
        state.getPlayerBid(playerId),
        seatPosition,
        visibleCards);
  }
}
