package com.yourco.ddz.server.api.dto;

import com.yourco.ddz.engine.core.GameState;
import java.util.List;

public record GameInfo(
    String gameId,
    String joinCode,
    List<PlayerInfo> players,
    int playerCount,
    String phase,
    String currentPlayer,
    String yourPlayerId,
    int maxBid) {
  public static GameInfo from(GameState state, String joinCode, int maxBid, int maxPlayers) {
    return from(state, joinCode, null, maxBid, maxPlayers);
  }

  public static GameInfo from(
      GameState state, String joinCode, String yourPlayerId, int maxBid, int maxPlayers) {
    return new GameInfo(
        state.gameId(),
        joinCode,
        state.players().stream().map(p -> PlayerInfo.from(state, p)).toList(),
        maxPlayers, // Use the actual max player count, not current count
        state.phase().name(),
        state.currentPlayerId() != null ? state.currentPlayerId().toString() : null,
        yourPlayerId,
        maxBid);
  }
}
