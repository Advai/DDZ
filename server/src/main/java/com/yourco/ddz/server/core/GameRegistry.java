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
  // Session tracking - maps sessionId to current GameInstance
  private final Map<String, GameInstance> sessionToInstance = new ConcurrentHashMap<>();
  // Map gameId to sessionId for lookups
  private final Map<String, String> gameIdToSessionId = new ConcurrentHashMap<>();
  private final Map<String, String> joinCodes = new ConcurrentHashMap<>(); // joinCode -> gameId
  // Track userId -> playerId mapping for each game
  private final Map<String, Map<UUID, UUID>> gameUserMappings =
      new ConcurrentHashMap<>(); // gameId -> (userId -> playerId)
  // Track game creators (gameId -> creatorPlayerId)
  private final Map<String, UUID> gameCreators = new ConcurrentHashMap<>();
  // Track seat positions (gameId -> (playerId -> seatPosition))
  private final Map<String, Map<UUID, Integer>> seatPositions = new ConcurrentHashMap<>();
  // Track creator tokens (creatorToken -> gameId)
  private final Map<String, String> creatorTokens = new ConcurrentHashMap<>();

  private final GamePersistenceService persistenceService;

  public GameRegistry(GamePersistenceService persistenceService) {
    this.persistenceService = persistenceService;
  }

  /**
   * Create a new empty game without any players.
   *
   * @param playerCount Number of players for this game
   * @return The created game instance
   */
  public GameInstance createGame(int playerCount) {
    String sessionId = "s-" + UUID.randomUUID().toString().substring(0, 8);
    String gameId = "g-" + UUID.randomUUID();
    String joinCode = generateUniqueJoinCode();
    String creatorToken = UUID.randomUUID().toString();

    var gameInstance = GameInstance.createEmpty(sessionId, gameId, playerCount);

    // Register in both old and new tracking maps
    games.put(gameId, gameInstance);
    sessionToInstance.put(sessionId, gameInstance);
    gameIdToSessionId.put(gameId, sessionId);

    joinCodes.put(joinCode, gameId);
    creatorTokens.put(creatorToken, gameId);

    // Initialize empty mappings for this game
    gameUserMappings.put(gameId, new ConcurrentHashMap<>());
    seatPositions.put(gameId, new ConcurrentHashMap<>());

    // Persist to database
    persistenceService.saveGame(gameInstance, new HashMap<>());

    log.info(
        "Created session {} with initial game {} with join code {} and {} max players",
        sessionId,
        gameId,
        joinCode,
        playerCount);

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

  public GameInstance get(String gameId) {
    return games.get(gameId);
  }

  public GameInstance getByJoinCode(String joinCode) {
    String gameId = joinCodes.get(joinCode);
    return gameId != null ? games.get(gameId) : null;
  }

  /**
   * Get game instance by session ID.
   *
   * @param sessionId The session ID
   * @return The current game instance for this session, or null if not found
   */
  public GameInstance getBySessionId(String sessionId) {
    return sessionToInstance.get(sessionId);
  }

  /**
   * Get session ID for a game ID.
   *
   * @param gameId The game ID
   * @return The session ID, or null if not found
   */
  public String getSessionId(String gameId) {
    return gameIdToSessionId.get(gameId);
  }

  /**
   * Register a new game ID for an existing session (used when starting a new round).
   *
   * @param gameId The new game ID
   * @param sessionId The existing session ID
   */
  public void registerGameId(String gameId, String sessionId) {
    GameInstance instance = sessionToInstance.get(sessionId);
    if (instance != null) {
      games.put(gameId, instance);
      gameIdToSessionId.put(gameId, sessionId);
      log.debug("Registered new game ID {} for session {}", gameId, sessionId);
    }
  }

  public String getJoinCode(String gameId) {
    return joinCodes.entrySet().stream()
        .filter(e -> e.getValue().equals(gameId))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(null);
  }

  /**
   * Get the creator token for a game.
   *
   * @param gameId The game ID
   * @return The creator token, or null if not found
   */
  public String getCreatorToken(String gameId) {
    return creatorTokens.entrySet().stream()
        .filter(e -> e.getValue().equals(gameId))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(null);
  }

  /**
   * Claim creator status for a game using a creator token.
   *
   * @param gameId The game ID
   * @param creatorToken The creator token to validate
   * @param playerId The player ID claiming creator status
   * @return true if the token was valid and creator status was claimed, false otherwise
   */
  public boolean claimCreatorStatus(String gameId, String creatorToken, UUID playerId) {
    String tokenGameId = creatorTokens.get(creatorToken);
    if (tokenGameId != null && tokenGameId.equals(gameId)) {
      // Valid token for this game - mark player as creator
      gameCreators.put(gameId, playerId);
      // Remove the token so it can only be used once
      creatorTokens.remove(creatorToken);
      log.info("Player {} claimed creator status for game {} using token", playerId, gameId);
      return true;
    }
    return false;
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

  /**
   * Check if a player is the creator of a game.
   *
   * @param gameId The game ID
   * @param playerId The player ID to check
   * @return true if the player is the creator, false otherwise
   */
  public boolean isGameCreator(String gameId, UUID playerId) {
    UUID creatorPlayerId = gameCreators.get(gameId);
    return creatorPlayerId != null && creatorPlayerId.equals(playerId);
  }

  /**
   * Get the creator's user ID for a game.
   *
   * @param gameId The game ID
   * @return The creator's user ID, or null if not found
   */
  public UUID getCreatorUserId(String gameId) {
    UUID creatorPlayerId = gameCreators.get(gameId);
    if (creatorPlayerId == null) {
      return null;
    }

    // Find the userId that maps to this playerId
    Map<UUID, UUID> userMappings = gameUserMappings.get(gameId);
    if (userMappings == null) {
      return null;
    }

    // Reverse lookup: find userId where playerId equals creatorPlayerId
    return userMappings.entrySet().stream()
        .filter(e -> e.getValue().equals(creatorPlayerId))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(null);
  }

  /**
   * Set a player's seat position.
   *
   * @param gameId The game ID
   * @param playerId The player ID
   * @param seatPosition The seat position (0-6)
   */
  public void setSeatPosition(String gameId, UUID playerId, int seatPosition) {
    seatPositions
        .computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
        .put(playerId, seatPosition);
    log.debug(
        "Set seat position for game {}: playerId={} -> seat={}", gameId, playerId, seatPosition);
  }

  /**
   * Get all seat positions for a game.
   *
   * @param gameId The game ID
   * @return Map of playerId -> seatPosition
   */
  public Map<UUID, Integer> getSeatPositions(String gameId) {
    return seatPositions.getOrDefault(gameId, new HashMap<>());
  }

  private String generateUniqueJoinCode() {
    String code;
    do {
      code = JoinCodeGenerator.generate();
    } while (joinCodes.containsKey(code));
    return code;
  }
}
