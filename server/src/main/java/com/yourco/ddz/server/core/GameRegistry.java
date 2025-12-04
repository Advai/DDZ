package com.yourco.ddz.server.core;

import com.yourco.ddz.server.service.GamePersistenceService;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GameRegistry {
  private static final Logger log = LoggerFactory.getLogger(GameRegistry.class);

  private final Map<String, GameInstance> games = new ConcurrentHashMap<>();
  private final Map<String, String> joinCodes = new ConcurrentHashMap<>(); // joinCode -> gameId
  // Track userId -> playerId mapping for each game
  private final Map<String, Map<UUID, UUID>> gameUserMappings =
      new ConcurrentHashMap<>(); // gameId -> (userId -> playerId)

  private final GamePersistenceService persistenceService;

  public GameRegistry(GamePersistenceService persistenceService) {
    this.persistenceService = persistenceService;
  }

  /**
   * Create a new game with a creator.
   *
   * @param playerCount Number of players for this game
   * @param creatorName Display name of the creator
   * @param creatorId Player ID for the creator (session-based)
   * @param userId User ID for the creator (persistent identity)
   * @return The created game instance
   */
  public GameInstance createGame(int playerCount, String creatorName, UUID creatorId, UUID userId) {
    String gameId = "g-" + UUID.randomUUID();
    String joinCode = generateUniqueJoinCode();

    var gameInstance = GameInstance.create(gameId, playerCount, creatorName, creatorId);

    games.put(gameId, gameInstance);
    joinCodes.put(joinCode, gameId);

    // Track userId -> playerId mapping
    Map<UUID, UUID> userMapping = new ConcurrentHashMap<>();
    userMapping.put(userId, creatorId);
    gameUserMappings.put(gameId, userMapping);

    // Persist to database
    persistenceService.saveGame(gameInstance, userMapping);
    persistenceService.addParticipant(gameId, userId, creatorId);

    log.info(
        "Created game {} with join code {} for user {} (player {})",
        gameId,
        joinCode,
        userId,
        creatorId);

    return gameInstance;
  }

  /**
   * Add a user mapping to a game (when a player joins).
   *
   * @param gameId The game ID
   * @param userId The user ID (persistent)
   * @param playerId The player ID (session)
   */
  public void addUserMapping(String gameId, UUID userId, UUID playerId) {
    gameUserMappings.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>()).put(userId, playerId);
    persistenceService.addParticipant(gameId, userId, playerId);
    log.debug("Added user mapping for game {}: userId={} -> playerId={}", gameId, userId, playerId);
  }

  /**
   * Update game state in database after an action.
   *
   * @param gameId The game ID
   */
  public void updateGame(String gameId) {
    GameInstance instance = games.get(gameId);
    if (instance != null) {
      Map<UUID, UUID> userMapping = gameUserMappings.getOrDefault(gameId, new HashMap<>());
      persistenceService.saveGame(instance, userMapping);
      log.debug("Updated game {} in database", gameId);
    }
  }

  /** Deprecated: Use createGame with userId parameter instead. */
  @Deprecated
  public GameInstance createGame(int playerCount, String creatorName, UUID creatorId) {
    // For backwards compatibility, create a temporary userId
    // This should not be used in production - all calls should provide userId
    log.warn(
        "createGame called without userId - this is deprecated and should be updated to pass"
            + " userId");
    return createGame(playerCount, creatorName, creatorId, UUID.randomUUID());
  }

  public GameInstance get(String gameId) {
    return games.get(gameId);
  }

  public GameInstance getByJoinCode(String joinCode) {
    String gameId = joinCodes.get(joinCode);
    return gameId != null ? games.get(gameId) : null;
  }

  public String getJoinCode(String gameId) {
    return joinCodes.entrySet().stream()
        .filter(e -> e.getValue().equals(gameId))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(null);
  }

  public Collection<GameInstance> getAllGames() {
    return games.values();
  }

  /**
   * Pause a game due to player disconnect.
   *
   * @param gameId The game ID to pause
   */
  public void pauseGame(String gameId) {
    persistenceService.pauseGame(gameId);
    log.info("Paused game {}", gameId);
  }

  /**
   * Resume a game when all players reconnect.
   *
   * @param gameId The game ID to resume
   */
  public void resumeGame(String gameId) {
    persistenceService.resumeGame(gameId);
    log.info("Resumed game {}", gameId);
  }

  /**
   * Get the active game for a user.
   *
   * @param userId The user ID
   * @return Optional containing the game ID if user has an active game
   */
  public Optional<String> getActiveGameForUser(UUID userId) {
    return persistenceService.getActiveGameForUser(userId);
  }

  private String generateUniqueJoinCode() {
    String code;
    do {
      code = JoinCodeGenerator.generate();
    } while (joinCodes.containsKey(code));
    return code;
  }
}
