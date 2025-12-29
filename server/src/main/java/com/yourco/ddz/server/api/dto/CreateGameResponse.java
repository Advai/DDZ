package com.yourco.ddz.server.api.dto;

public record CreateGameResponse(
    String sessionId, String gameId, String joinCode, String creatorToken) {}
