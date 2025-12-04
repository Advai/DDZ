package com.yourco.ddz.server.api.dto;

import java.util.UUID;

public record JoinGameRequest(String playerName, UUID userId) {
  public JoinGameRequest {
    if (playerName == null || playerName.isBlank()) {
      throw new IllegalArgumentException("Player name is required");
    }
    if (userId == null) {
      throw new IllegalArgumentException("User ID is required");
    }
  }
}
