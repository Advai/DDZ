package com.yourco.ddz.server.api;

import com.yourco.ddz.engine.core.GameState;
import com.yourco.ddz.engine.core.SystemAction;
import com.yourco.ddz.server.api.dto.*;
import com.yourco.ddz.server.core.GameRegistry;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow all origins for now (no auth yet)
public class GameController {
  private static final Logger log = LoggerFactory.getLogger(GameController.class);

  private final GameRegistry registry;
  private final com.yourco.ddz.server.ws.GameWebSocketHandler wsHandler;
  private final com.yourco.ddz.server.service.UserService userService;
  private final com.yourco.ddz.server.repository.GameResultRepository gameResultRepository;

  public GameController(
      GameRegistry r,
      com.yourco.ddz.server.ws.GameWebSocketHandler ws,
      com.yourco.ddz.server.service.UserService userService,
      com.yourco.ddz.server.repository.GameResultRepository gameResultRepository) {
    this.registry = r;
    this.wsHandler = ws;
    this.userService = userService;
    this.gameResultRepository = gameResultRepository;
  }

  @PostMapping("/auth/login")
  public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    try {
      var user = userService.getOrCreateUser(request.username(), request.displayName());
      var response = new LoginResponse(user.getUserId(), user.getUsername(), user.getDisplayName());
      log.info("User logged in: {} ({})", user.getUsername(), user.getUserId());
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      log.warn("Login failed: {}", e.getMessage());
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  @PostMapping("/games")
  public ResponseEntity<?> createGame(@RequestBody CreateGameRequest request) {
    var instance = registry.createGame(request.playerCount());
    String sessionId = instance.getSessionId();
    String gameId = instance.getCurrentGameId();
    String joinCode = registry.getJoinCode(gameId);
    String creatorToken = registry.getCreatorToken(gameId);

    var response = new CreateGameResponse(sessionId, gameId, joinCode, creatorToken);

    log.info("Created session {} with game {} and join code {}", sessionId, gameId, joinCode);

    return ResponseEntity.ok(response);
  }

  @PostMapping("/games/{gameId}/join")
  public ResponseEntity<?> joinGame(
      @PathVariable String gameId, @RequestBody JoinGameRequest request) {
    // Support both sessionId and gameId lookups
    var instance = registry.getBySessionId(gameId);
    String actualGameId = gameId;

    if (instance == null) {
      instance = registry.get(gameId);
      if (instance != null) {
        actualGameId = instance.getCurrentGameId();
      }
    } else {
      actualGameId = instance.getCurrentGameId();
    }

    if (instance == null) {
      return ResponseEntity.notFound().build();
    }

    if (instance.isFull()) {
      return ResponseEntity.badRequest().body("Game is full");
    }

    // Check if seat is already occupied
    if (request.seatPosition() != null) {
      var seatPositions = registry.getSeatPositions(actualGameId);
      boolean seatTaken =
          seatPositions.values().stream().anyMatch(pos -> pos.equals(request.seatPosition()));

      if (seatTaken) {
        return ResponseEntity.badRequest()
            .body("Seat " + request.seatPosition() + " is already taken");
      }
    }

    // Add player to game
    UUID playerId = UUID.randomUUID();
    instance.getState().addPlayer(playerId, request.playerName());

    // Track userId -> playerId mapping for reconnection
    registry.addUserMapping(actualGameId, request.userId(), playerId);

    // Set seat position if provided
    if (request.seatPosition() != null) {
      registry.setSeatPosition(actualGameId, playerId, request.seatPosition());
    }

    // If creatorToken provided, claim creator status
    if (request.creatorToken() != null) {
      boolean claimed = registry.claimCreatorStatus(actualGameId, request.creatorToken(), playerId);
      if (claimed) {
        log.info("Player {} claimed creator status for game {}", playerId, actualGameId);
      } else {
        log.warn("Player {} attempted to claim creator status with invalid token", playerId);
      }
    }

    String joinCode = registry.getJoinCode(actualGameId);

    // Get seat positions for all players
    var seatPositions = registry.getSeatPositions(actualGameId);

    // Get creator user ID
    UUID creatorUserId = registry.getCreatorUserId(actualGameId);
    String creatorId = creatorUserId != null ? creatorUserId.toString() : null;

    var response =
        GameInfo.fromWithSeats(
            instance.getState(),
            joinCode,
            playerId.toString(),
            creatorId,
            instance.getMaxBid(),
            instance.maxPlayers(),
            seatPositions);

    // Broadcast to all WebSocket clients that a new player joined
    wsHandler.broadcastStateUpdate(
        instance.getSessionId(), instance, request.playerName() + " joined the game");

    log.info(
        "Player {} ({}) joined game {} with userId {}",
        request.playerName(),
        playerId,
        gameId,
        request.userId());

    return ResponseEntity.ok(response);
  }

  @PostMapping("/games/{gameId}/start")
  public ResponseEntity<?> startGame(@PathVariable String gameId, @RequestParam String playerId) {
    // Support both sessionId and gameId lookups
    var instance = registry.getBySessionId(gameId);
    String actualGameId = gameId;

    if (instance == null) {
      instance = registry.get(gameId);
      if (instance != null) {
        actualGameId = instance.getCurrentGameId();
      }
    } else {
      actualGameId = instance.getCurrentGameId();
    }

    if (instance == null) {
      log.error("Cannot start game - game/session not found: {}", gameId);
      return ResponseEntity.notFound().build();
    }

    // Validate playerId format
    try {
      UUID.fromString(playerId);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body("Invalid playerId");
    }

    int currentPlayers = instance.getState().players().size();

    // Require minimum 3 players to start
    if (currentPlayers < 3) {
      String errorMsg = "Cannot start game. Need at least 3 players, have " + currentPlayers;
      log.warn("Cannot start game {}: {}", actualGameId, errorMsg);
      return ResponseEntity.badRequest().body(errorMsg);
    }

    log.info("Start game request for {} - Current players: {}", actualGameId, currentPlayers);

    // Allow starting from LOBBY or restarting from TERMINATED
    if (instance.getState().phase() != GameState.Phase.LOBBY
        && instance.getState().phase() != GameState.Phase.TERMINATED) {
      return ResponseEntity.badRequest().body("Game already in progress");
    }

    // Start or restart the game (this resets phase to LOBBY if restarting)
    boolean isRestart = instance.getState().phase() == GameState.Phase.TERMINATED;

    // If restarting, reset the scored flag so tick() can process actions
    if (isRestart) {
      instance.loop().resetForNewRound();
    }

    instance.loop().submit(new SystemAction("START", null));

    // IMPORTANT: Reconfigure rules BEFORE tick() - must be done while still in LOBBY/TERMINATED
    instance.reconfigureForPlayerCount(currentPlayers);
    log.info(
        "Reconfigured game {} for {} players - Max bid: {}",
        actualGameId,
        currentPlayers,
        instance.getMaxBid());

    // Now process the START action
    instance.loop().tick();

    // Persist the updated state
    registry.updateGame(actualGameId);

    String joinCode = registry.getJoinCode(actualGameId);

    // Get creator user ID
    UUID creatorUserId = registry.getCreatorUserId(actualGameId);
    String creatorId = creatorUserId != null ? creatorUserId.toString() : null;

    var response =
        GameInfo.from(
            instance.getState(),
            joinCode,
            null,
            creatorId,
            instance.getMaxBid(),
            instance.maxPlayers());

    // Log the state change
    String action = isRestart ? "restarted" : "started";
    log.info(
        "Game {} {} - Phase: {}, Current Player: {}",
        actualGameId,
        action,
        instance.getState().phase(),
        instance.getState().currentPlayerId());

    // Broadcast to all WebSocket clients
    String message =
        isRestart ? "New game started!" : "Game started - Phase: " + instance.getState().phase();
    wsHandler.broadcastStateUpdate(instance.getSessionId(), instance, message);

    return ResponseEntity.ok(response);
  }

  @PostMapping("/games/{sessionId}/restart")
  public ResponseEntity<?> restartRound(@PathVariable String sessionId) {
    var instance = registry.getBySessionId(sessionId);

    if (instance == null) {
      log.error("Cannot restart - session not found: {}", sessionId);
      return ResponseEntity.notFound().build();
    }

    // Only allow restart from TERMINATED phase
    if (instance.getState().phase() != GameState.Phase.TERMINATED) {
      return ResponseEntity.badRequest().body("Can only restart from TERMINATED phase");
    }

    // Generate new game ID for new round
    String newGameId = "g-" + UUID.randomUUID();
    instance.incrementRound(newGameId);
    registry.registerGameId(newGameId, sessionId);

    int currentPlayers = instance.getState().players().size();
    log.info(
        "Restarting session {} - New round {} (gameId: {}) with {} players",
        sessionId,
        instance.getRoundNumber(),
        newGameId,
        currentPlayers);

    // Reset and start the game (reconfigure BEFORE tick)
    instance.loop().submit(new SystemAction("START", null));
    instance.reconfigureForPlayerCount(currentPlayers);
    instance.loop().tick();

    // Persist the new round to database
    registry.updateGame(newGameId);

    log.info(
        "Started round {} for session {} - Phase: {}, Current Player: {}",
        instance.getRoundNumber(),
        sessionId,
        instance.getState().phase(),
        instance.getState().currentPlayerId());

    // Broadcast to all WebSocket clients
    wsHandler.broadcastStateUpdate(
        sessionId, instance, "Round " + instance.getRoundNumber() + " started!");

    return ResponseEntity.ok(
        java.util.Map.of(
            "sessionId", sessionId,
            "gameId", newGameId,
            "roundNumber", instance.getRoundNumber()));
  }

  @GetMapping("/games/{gameId}")
  public ResponseEntity<?> getBasicGameInfo(@PathVariable String gameId) {
    // Support both sessionId and gameId lookups
    var instance = registry.getBySessionId(gameId);
    String actualGameId = gameId;

    if (instance == null) {
      instance = registry.get(gameId);
      if (instance != null) {
        actualGameId = instance.getCurrentGameId();
      }
    } else {
      actualGameId = instance.getCurrentGameId();
    }

    if (instance == null) {
      return ResponseEntity.notFound().build();
    }

    String joinCode = registry.getJoinCode(actualGameId);
    var seatPositions = registry.getSeatPositions(actualGameId);

    // Get creator user ID
    UUID creatorUserId = registry.getCreatorUserId(actualGameId);
    String creatorId = creatorUserId != null ? creatorUserId.toString() : null;

    var response =
        GameInfo.fromWithSeats(
            instance.getState(),
            joinCode,
            null,
            creatorId,
            instance.getMaxBid(),
            instance.maxPlayers(),
            seatPositions);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/games/{gameId}/state")
  public ResponseEntity<?> getGameState(
      @PathVariable String gameId, @RequestParam String playerId) {
    // Support both sessionId and gameId lookups
    var instance = registry.getBySessionId(gameId);
    if (instance == null) {
      instance = registry.get(gameId);
    }

    if (instance == null) {
      return ResponseEntity.notFound().build();
    }

    UUID playerUUID;
    try {
      playerUUID = UUID.fromString(playerId);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body("Invalid playerId");
    }

    if (!instance.getState().players().contains(playerUUID)) {
      return ResponseEntity.badRequest().body("Player not in this game");
    }

    var response =
        GameStateResponse.from(
            instance.getState(), playerUUID, instance.getMaxBid(), instance.maxPlayers());
    return ResponseEntity.ok(response);
  }

  @GetMapping("/games/by-code/{joinCode}")
  public ResponseEntity<?> getGameByJoinCode(@PathVariable String joinCode) {
    var instance = registry.getByJoinCode(joinCode);

    if (instance == null) {
      return ResponseEntity.notFound().build();
    }

    String gameId = instance.getState().gameId();

    // Get creator user ID
    UUID creatorUserId = registry.getCreatorUserId(gameId);
    String creatorId = creatorUserId != null ? creatorUserId.toString() : null;

    var response =
        GameInfo.from(
            instance.getState(),
            joinCode,
            null,
            creatorId,
            instance.getMaxBid(),
            instance.maxPlayers());
    return ResponseEntity.ok(response);
  }

  @GetMapping("/users/{userId}/active-game")
  public ResponseEntity<?> getActiveGame(@PathVariable UUID userId) {
    var activeGameId = registry.getActiveGameForUser(userId);

    if (activeGameId.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    String gameId = activeGameId.get();
    var instance = registry.get(gameId);

    if (instance == null) {
      log.warn("Active game {} for user {} not found in registry", gameId, userId);
      return ResponseEntity.notFound().build();
    }

    String joinCode = registry.getJoinCode(gameId);
    var response =
        GameInfo.from(instance.getState(), joinCode, instance.getMaxBid(), instance.maxPlayers());
    return ResponseEntity.ok(response);
  }

  @GetMapping("/games/{sessionId}/session-stats")
  public ResponseEntity<?> getSessionStats(@PathVariable String sessionId) {
    var stats = gameResultRepository.getSessionStats(sessionId);
    return ResponseEntity.ok(stats);
  }
}
