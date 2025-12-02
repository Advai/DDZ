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
@RequestMapping("/api/games")
@CrossOrigin(origins = "*") // Allow all origins for now (no auth yet)
public class GameController {
  private static final Logger log = LoggerFactory.getLogger(GameController.class);

  private final GameRegistry registry;
  private final com.yourco.ddz.server.ws.GameWebSocketHandler wsHandler;

  public GameController(GameRegistry r, com.yourco.ddz.server.ws.GameWebSocketHandler ws) {
    this.registry = r;
    this.wsHandler = ws;
  }

  @PostMapping
  public ResponseEntity<?> createGame(@RequestBody CreateGameRequest request) {
    UUID creatorId = UUID.randomUUID();
    var instance = registry.createGame(request.playerCount(), request.creatorName(), creatorId);
    String joinCode = registry.getJoinCode(instance.gameId());

    var response =
        GameInfo.from(
            instance.getState(),
            joinCode,
            creatorId.toString(),
            instance.getMaxBid(),
            instance.maxPlayers());

    // Broadcast to any connected WebSocket clients (usually none for new games, but just in case)
    wsHandler.broadcastStateUpdate(
        instance.gameId(), instance, "Game created by " + request.creatorName());

    return ResponseEntity.ok(response);
  }

  @PostMapping("/{gameId}/join")
  public ResponseEntity<?> joinGame(
      @PathVariable String gameId, @RequestBody JoinGameRequest request) {
    var instance = registry.get(gameId);

    if (instance == null) {
      return ResponseEntity.notFound().build();
    }

    if (instance.isFull()) {
      return ResponseEntity.badRequest().body("Game is full");
    }

    // Add player to game
    UUID playerId = UUID.randomUUID();
    instance.getState().addPlayer(playerId, request.playerName());

    String joinCode = registry.getJoinCode(gameId);
    var response =
        GameInfo.from(
            instance.getState(),
            joinCode,
            playerId.toString(),
            instance.getMaxBid(),
            instance.maxPlayers());

    // Broadcast to all WebSocket clients that a new player joined
    wsHandler.broadcastStateUpdate(gameId, instance, request.playerName() + " joined the game");

    log.info("Player {} ({}) joined game {}", request.playerName(), playerId, gameId);

    return ResponseEntity.ok(response);
  }

  @PostMapping("/{gameId}/start")
  public ResponseEntity<?> startGame(@PathVariable String gameId) {
    var instance = registry.get(gameId);

    if (instance == null) {
      log.error("Cannot start game - game not found: {}", gameId);
      return ResponseEntity.notFound().build();
    }

    int currentPlayers = instance.getState().players().size();
    int requiredPlayers = instance.maxPlayers();

    log.info(
        "Start game request for {} - Current players: {}, Required: {}, Can start: {}",
        gameId,
        currentPlayers,
        requiredPlayers,
        instance.canStart());

    if (!instance.canStart()) {
      String errorMsg =
          "Cannot start game. Need " + requiredPlayers + " players, have " + currentPlayers;
      log.warn("Cannot start game {}: {}", gameId, errorMsg);
      return ResponseEntity.badRequest().body(errorMsg);
    }

    // Allow starting from LOBBY or restarting from TERMINATED
    if (instance.getState().phase() != GameState.Phase.LOBBY
        && instance.getState().phase() != GameState.Phase.TERMINATED) {
      return ResponseEntity.badRequest().body("Game already in progress");
    }

    // Start or restart the game
    boolean isRestart = instance.getState().phase() == GameState.Phase.TERMINATED;
    instance.loop().submit(new SystemAction("START", null));
    instance.loop().tick();

    String joinCode = registry.getJoinCode(gameId);
    var response =
        GameInfo.from(instance.getState(), joinCode, instance.getMaxBid(), instance.maxPlayers());

    // Log the state change
    String action = isRestart ? "restarted" : "started";
    log.info(
        "Game {} {} - Phase: {}, Current Player: {}",
        gameId,
        action,
        instance.getState().phase(),
        instance.getState().currentPlayerId());

    // Broadcast to all WebSocket clients
    String message =
        isRestart ? "New game started!" : "Game started - Phase: " + instance.getState().phase();
    wsHandler.broadcastStateUpdate(gameId, instance, message);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/{gameId}/state")
  public ResponseEntity<?> getGameState(
      @PathVariable String gameId, @RequestParam String playerId) {
    var instance = registry.get(gameId);

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

  @GetMapping("/by-code/{joinCode}")
  public ResponseEntity<?> getGameByJoinCode(@PathVariable String joinCode) {
    var instance = registry.getByJoinCode(joinCode);

    if (instance == null) {
      return ResponseEntity.notFound().build();
    }

    var response =
        GameInfo.from(instance.getState(), joinCode, instance.getMaxBid(), instance.maxPlayers());
    return ResponseEntity.ok(response);
  }
}
