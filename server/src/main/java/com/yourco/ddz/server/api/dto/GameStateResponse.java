package com.yourco.ddz.server.api.dto;

import com.yourco.ddz.engine.core.GameState;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public record GameStateResponse(
    String gameId,
    String phase,
    String currentPlayer,
    List<CardDto> myHand,
    List<PlayerInfo> players,
    PlayedHandDto currentLead,
    Map<String, Integer> scores,
    Integer bombsPlayed,
    Integer rocketsPlayed,
    Integer currentBet,
    Integer multiplier) {

  public static GameStateResponse from(GameState state, UUID requestingPlayerId) {
    // Convert scores to string keys
    Map<String, Integer> scoresMap =
        state.getScores().entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue));

    // Calculate multiplier: base bet * 2^(bombs + rockets)
    int currentBet = state.getHighestBid();
    int multiplier =
        currentBet * (int) Math.pow(2, state.getBombsPlayed() + state.getRocketsPlayed());

    return new GameStateResponse(
        state.gameId(),
        state.phase().name(),
        state.currentPlayerId() != null ? state.currentPlayerId().toString() : null,
        state.handOf(requestingPlayerId).stream().map(CardDto::from).toList(),
        state.players().stream().map(p -> PlayerInfo.from(state, p)).toList(),
        PlayedHandDto.from(state.getCurrentLead()),
        scoresMap,
        state.getBombsPlayed(),
        state.getRocketsPlayed(),
        currentBet,
        multiplier);
  }
}
