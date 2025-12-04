package com.yourco.ddz.server.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "game_results")
public class GameResult {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "game_id", nullable = false, length = 50)
  private String gameId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "player_id", nullable = false)
  private UUID playerId;

  @Column(name = "final_score", nullable = false)
  private int finalScore;

  @Column(name = "was_landlord", nullable = false)
  private boolean wasLandlord;

  @Column(name = "completed_at", nullable = false)
  private Instant completedAt;

  public GameResult() {
    this.completedAt = Instant.now();
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

  public int getFinalScore() {
    return finalScore;
  }

  public void setFinalScore(int finalScore) {
    this.finalScore = finalScore;
  }

  public boolean isWasLandlord() {
    return wasLandlord;
  }

  public void setWasLandlord(boolean wasLandlord) {
    this.wasLandlord = wasLandlord;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(Instant completedAt) {
    this.completedAt = completedAt;
  }
}
