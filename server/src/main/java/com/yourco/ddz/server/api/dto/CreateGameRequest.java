package com.yourco.ddz.server.api.dto;

import java.util.UUID;

public record CreateGameRequest(int playerCount, String creatorName, UUID userId) {
  public CreateGameRequest {
    if (playerCount < 3 || playerCount > 12) {
      throw new IllegalArgumentException("Player count must be between 3 and 12");
    }
    if (creatorName == null || creatorName.isBlank()) {
      throw new IllegalArgumentException("Creator name is required");
    }
    if (userId == null) {
      throw new IllegalArgumentException("User ID is required");
    }
  }
}
