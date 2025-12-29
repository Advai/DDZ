package com.yourco.ddz.server.api.dto;

public record CreateGameRequest(int playerCount) {
  public CreateGameRequest {
    if (playerCount < 3 || playerCount > 7) {
      throw new IllegalArgumentException("Player count must be between 3 and 7");
    }
  }
}
