package com.yourco.ddz.server.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "game_participants")
public class GameParticipant {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "game_id", nullable = false, length = 50)
  private String gameId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "player_id", nullable = false)
  private UUID playerId;

  @Column(name = "joined_at", nullable = false)
  private Instant joinedAt;

  @Column(name = "left_at")
  private Instant leftAt;

  public GameParticipant() {
    this.joinedAt = Instant.now();
  }

  public GameParticipant(String gameId, UUID userId, UUID playerId) {
    this();
    this.gameId = gameId;
    this.userId = userId;
    this.playerId = playerId;
  }

  // Getters and setters
  public Long getId() {
    return id;
  }

  public String getGameId() {
    return gameId;
  }

  public void setGameId(String gameId) {
    this.gameId = gameId;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public UUID getPlayerId() {
    return playerId;
  }

  public void setPlayerId(UUID playerId) {
    this.playerId = playerId;
  }

  public Instant getJoinedAt() {
    return joinedAt;
  }

  public Instant getLeftAt() {
    return leftAt;
  }

  public void setLeftAt(Instant leftAt) {
    this.leftAt = leftAt;
  }

  public boolean isActive() {
    return leftAt == null;
  }
}
