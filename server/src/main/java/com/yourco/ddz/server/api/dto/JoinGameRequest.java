package com.yourco.ddz.server.api.dto;

public record JoinGameRequest(String playerName) {
  public JoinGameRequest {
    if (playerName == null || playerName.isBlank()) {
      throw new IllegalArgumentException("Player name is required");
    }
  }
}
