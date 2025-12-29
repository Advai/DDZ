package com.yourco.ddz.server.api.dto;

import java.util.UUID;

public record JoinGameRequest(
    String playerName, UUID userId, Integer seatPosition, String creatorToken) {
  public JoinGameRequest {
    if (playerName == null || playerName.isBlank()) {
      throw new IllegalArgumentException("Player name is required");
    }
    if (userId == null) {
      throw new IllegalArgumentException("User ID is required");
    }
    if (seatPosition != null && (seatPosition < 0 || seatPosition >= 7)) {
      throw new IllegalArgumentException("Seat position must be between 0 and 6");
    }
    // creatorToken is optional, no validation needed
  }
}
