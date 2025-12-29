package com.yourco.ddz.server.api.dto;

import com.yourco.ddz.engine.core.GameState;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record GameInfo(
    String gameId,
    String joinCode,
    List<PlayerInfo> players,
    int playerCount,
    String phase,
    String currentPlayer,
    String yourPlayerId,
    String creatorId,
    int maxBid) {
  public static GameInfo from(GameState state, String joinCode, int maxBid, int maxPlayers) {
    return from(state, joinCode, null, null, maxBid, maxPlayers);
  }

  public static GameInfo from(
      GameState state, String joinCode, String yourPlayerId, int maxBid, int maxPlayers) {
    return from(state, joinCode, yourPlayerId, null, maxBid, maxPlayers);
  }

  public static GameInfo from(
      GameState state,
      String joinCode,
      String yourPlayerId,
      String creatorId,
      int maxBid,
      int maxPlayers) {
    return new GameInfo(
        state.gameId(),
        joinCode,
        state.players().stream().map(p -> PlayerInfo.from(state, p)).toList(),
        maxPlayers, // Use the actual max player count, not current count
        state.phase().name(),
        state.currentPlayerId() != null ? state.currentPlayerId().toString() : null,
        yourPlayerId,
        creatorId,
        maxBid);
  }

  public static GameInfo fromWithSeats(
      GameState state,
      String joinCode,
      String yourPlayerId,
      String creatorId,
      int maxBid,
      int maxPlayers,
      Map<UUID, Integer> seatPositions) {
    return new GameInfo(
        state.gameId(),
        joinCode,
        state.players().stream().map(p -> PlayerInfo.from(state, p, seatPositions.get(p))).toList(),
        maxPlayers,
        state.phase().name(),
        state.currentPlayerId() != null ? state.currentPlayerId().toString() : null,
        yourPlayerId,
        creatorId,
        maxBid);
  }
}
