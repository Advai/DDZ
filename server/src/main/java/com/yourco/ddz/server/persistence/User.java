package com.yourco.ddz.server.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "username", unique = true, nullable = false, length = 50)
  private String username;

  @Column(name = "display_name", nullable = false, length = 100)
  private String displayName;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "last_seen_at", nullable = false)
  private Instant lastSeenAt;

  public User() {
    this.createdAt = Instant.now();
    this.lastSeenAt = Instant.now();
  }

  public User(String username, String displayName) {
    this();
    this.username = username;
    this.displayName = displayName;
  }

  // Getters and setters
  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getLastSeenAt() {
    return lastSeenAt;
  }

  public void setLastSeenAt(Instant lastSeenAt) {
    this.lastSeenAt = lastSeenAt;
  }

  public void updateLastSeen() {
    this.lastSeenAt = Instant.now();
  }
}
