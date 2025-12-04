package com.yourco.ddz.server.api.dto;

public record LoginRequest(String username, String displayName) {
  public LoginRequest {
    if (username == null || username.isBlank()) {
      throw new IllegalArgumentException("Username is required");
    }
    if (displayName == null || displayName.isBlank()) {
      throw new IllegalArgumentException("Display name is required");
    }
  }
}
