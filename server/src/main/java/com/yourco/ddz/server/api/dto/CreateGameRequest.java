package com.yourco.ddz.server.api.dto;

public record CreateGameRequest(int playerCount, String creatorName) {
  public CreateGameRequest {
    if (playerCount < 3 || playerCount > 12) {
      throw new IllegalArgumentException("Player count must be between 3 and 12");
    }
    if (creatorName == null || creatorName.isBlank()) {
      throw new IllegalArgumentException("Creator name is required");
    }
  }
}
