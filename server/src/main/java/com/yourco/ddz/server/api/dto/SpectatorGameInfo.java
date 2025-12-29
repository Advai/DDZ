package com.yourco.ddz.server.api.dto;

import java.util.List;

public record SpectatorGameInfo(String gameId, String phase, List<BasicPlayerInfo> players) {
  public record BasicPlayerInfo(String id, String name, Integer seatPosition) {}
}
