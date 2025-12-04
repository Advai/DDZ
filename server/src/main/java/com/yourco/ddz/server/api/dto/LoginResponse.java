package com.yourco.ddz.server.api.dto;

import java.util.UUID;

public record LoginResponse(UUID userId, String username, String displayName) {}
