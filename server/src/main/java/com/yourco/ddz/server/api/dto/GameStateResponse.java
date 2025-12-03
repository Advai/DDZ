package com.yourco.ddz.server.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yourco.ddz.engine.core.GameState;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public record GameStateResponse(
    @JsonProperty("gameId") String gameId,
    @JsonProperty("phase") String phase,
    @JsonProperty("currentPlayer") String currentPlayer,
    @JsonProperty("myHand") List<CardDto> myHand,
    @JsonProperty("players") List<PlayerInfo> players,
    @JsonProperty("playerCount") int playerCount,
    @JsonProperty("currentLead") PlayedHandDto currentLead,
    @JsonProperty("scores") Map<String, Integer> scores,
    @JsonProperty("bombsPlayed") Integer bombsPlayed,
    @JsonProperty("rocketsPlayed") Integer rocketsPlayed,
    @JsonProperty("currentBet") Integer currentBet,
    @JsonProperty("multiplier") Integer multiplier,
    @JsonProperty("maxBid") Integer maxBid,
    @JsonProperty("landlordIds") List<String> landlordIds,
    @JsonProperty("awaitingLandlordSelection") String awaitingLandlordSelection) {

  public static GameStateResponse from(
      GameState state, UUID requestingPlayerId, int maxBid, int maxPlayers) {
    // Convert scores to string keys
    Map<String, Integer> scoresMap =
        state.getScores().entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue));

    // Calculate multiplier: base bet * 2^(bombs + rockets)
    int currentBet = state.getHighestBid();
    int multiplier =
        currentBet * (int) Math.pow(2, state.getBombsPlayed() + state.getRocketsPlayed());

    // Convert landlord IDs to strings
    List<String> landlordIds = state.getLandlordIds().stream().map(UUID::toString).toList();

    // Get awaiting landlord selection (if any)
    String awaitingLandlordSelection =
        state.getAwaitingLandlordSelection() != null
            ? state.getAwaitingLandlordSelection().toString()
            : null;

    return new GameStateResponse(
        state.gameId(),
        state.phase().name(),
        state.currentPlayerId() != null ? state.currentPlayerId().toString() : null,
        state.handOf(requestingPlayerId).stream().map(CardDto::from).toList(),
        state.players().stream().map(p -> PlayerInfo.from(state, p)).toList(),
        maxPlayers,
        PlayedHandDto.from(state.getCurrentLead()),
        scoresMap,
        state.getBombsPlayed(),
        state.getRocketsPlayed(),
        currentBet,
        multiplier,
        maxBid,
        landlordIds,
        awaitingLandlordSelection);
  }
}
