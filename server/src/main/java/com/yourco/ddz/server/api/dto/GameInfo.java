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
    String yourPlayerId) {
  public static GameInfo from(GameState state, String joinCode) {
    return from(state, joinCode, null);
  }

  public static GameInfo from(GameState state, String joinCode, String yourPlayerId) {
    return new GameInfo(
        state.gameId(),
        joinCode,
        state.players().stream().map(p -> PlayerInfo.from(state, p)).toList(),
        state.players().size(), // TODO: track max separately for lobby
        state.phase().name(),
        state.currentPlayerId() != null ? state.currentPlayerId().toString() : null,
        yourPlayerId);
  }
}
