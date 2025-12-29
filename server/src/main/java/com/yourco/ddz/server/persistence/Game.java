package com.yourco.ddz.server.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "games")
public class Game {
  @Id
  @Column(name = "game_id", length = 50)
  private String gameId;

  @Column(name = "session_id", nullable = false, length = 50)
  private String sessionId;

  @Column(name = "round_number", nullable = false)
  private int roundNumber = 1;

  @Column(name = "join_code", unique = true, nullable = false, length = 4)
  private String joinCode;

  @Column(name = "max_players", nullable = false)
  private int maxPlayers;

  @Column(name = "current_phase", nullable = false, length = 20)
  private String currentPhase;

  @Column(name = "is_paused", nullable = false)
  private boolean isPaused;

  @Type(JsonBinaryType.class)
  @Column(name = "game_state_json", columnDefinition = "jsonb", nullable = false)
  private JsonNode gameStateJson;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  public Game() {
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
    this.isPaused = false;
  }

  // Getters and setters
  public String getGameId() {
    return gameId;
  }

  public void setGameId(String gameId) {
    this.gameId = gameId;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public int getRoundNumber() {
    return roundNumber;
  }

  public void setRoundNumber(int roundNumber) {
    this.roundNumber = roundNumber;
  }

  public String getJoinCode() {
    return joinCode;
  }

  public void setJoinCode(String joinCode) {
    this.joinCode = joinCode;
  }

  public int getMaxPlayers() {
    return maxPlayers;
  }

  public void setMaxPlayers(int maxPlayers) {
    this.maxPlayers = maxPlayers;
  }

  public String getCurrentPhase() {
    return currentPhase;
  }

  public void setCurrentPhase(String currentPhase) {
    this.currentPhase = currentPhase;
  }

  public boolean isPaused() {
    return isPaused;
  }

  public void setPaused(boolean paused) {
    isPaused = paused;
  }

  public JsonNode getGameStateJson() {
    return gameStateJson;
  }

  public void setGameStateJson(JsonNode gameStateJson) {
    this.gameStateJson = gameStateJson;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(Instant completedAt) {
    this.completedAt = completedAt;
  }

  @PreUpdate
  public void preUpdate() {
    this.updatedAt = Instant.now();
  }
}
