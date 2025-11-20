package com.yourco.ddz.server.api.dto;

import com.yourco.ddz.engine.core.GameState;
import java.util.UUID;

public record PlayerInfo(
    String id, String name, int cardCount, boolean isLandlord, boolean isConnected, int score) {
  public static PlayerInfo from(GameState state, UUID playerId) {
    return new PlayerInfo(
        playerId.toString(),
        state.getPlayerName(playerId),
        state.handOf(playerId).size(),
        state.isLandlord(playerId),
        state.isPlayerConnected(playerId),
        state.getScores().getOrDefault(playerId, 0));
  }
}
