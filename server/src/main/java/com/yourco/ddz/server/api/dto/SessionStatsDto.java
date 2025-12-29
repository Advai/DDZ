package com.yourco.ddz.server.api.dto;

public record SessionStatsDto(
    String userId,
    String username,
    String displayName,
    Long totalPoints,
    Long landlordWins,
    Long peasantWins,
    Long totalWins,
    Long gamesPlayed) {
  public static SessionStatsDto create(
      String userId,
      String username,
      String displayName,
      Long totalPoints,
      Long landlordWins,
      Long peasantWins) {
    return new SessionStatsDto(
        userId,
        username,
        displayName,
        totalPoints,
        landlordWins,
        peasantWins,
        landlordWins + peasantWins,
        landlordWins + peasantWins);
  }
}
