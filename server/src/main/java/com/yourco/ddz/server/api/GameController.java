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

    var response = GameInfo.from(instance.getState(), joinCode, creatorId.toString());

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
    var response = GameInfo.from(instance.getState(), joinCode, playerId.toString());

    // Broadcast to all WebSocket clients that a new player joined
    wsHandler.broadcastStateUpdate(gameId, instance, request.playerName() + " joined the game");

    log.info("Player {} ({}) joined game {}", request.playerName(), playerId, gameId);

    return ResponseEntity.ok(response);
  }

  @PostMapping("/{gameId}/start")
  public ResponseEntity<?> startGame(@PathVariable String gameId) {
    var instance = registry.get(gameId);

    if (instance == null) {
      return ResponseEntity.notFound().build();
    }

    if (!instance.canStart()) {
      return ResponseEntity.badRequest()
          .body(
              "Cannot start game. Need "
                  + instance.maxPlayers()
                  + " players, have "
                  + instance.getState().players().size());
    }

    if (instance.getState().phase() != GameState.Phase.LOBBY) {
      return ResponseEntity.badRequest().body("Game already started");
    }

    // Start the game
    instance.loop().submit(new SystemAction("START", null));
    instance.loop().tick();

    String joinCode = registry.getJoinCode(gameId);
    var response = GameInfo.from(instance.getState(), joinCode);

    // Log the state change
    log.info(
        "Game {} started - Phase: {}, Current Player: {}",
        gameId,
        instance.getState().phase(),
        instance.getState().currentPlayerId());

    // Broadcast to all WebSocket clients
    wsHandler.broadcastStateUpdate(
        gameId, instance, "Game started - Phase: " + instance.getState().phase());

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

    var response = GameStateResponse.from(instance.getState(), playerUUID);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/by-code/{joinCode}")
  public ResponseEntity<?> getGameByJoinCode(@PathVariable String joinCode) {
    var instance = registry.getByJoinCode(joinCode);

    if (instance == null) {
      return ResponseEntity.notFound().build();
    }

    var response = GameInfo.from(instance.getState(), joinCode);
    return ResponseEntity.ok(response);
  }
}
