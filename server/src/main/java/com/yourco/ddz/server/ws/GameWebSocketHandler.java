package com.yourco.ddz.server.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourco.ddz.engine.cards.Card;
import com.yourco.ddz.engine.core.*;
import com.yourco.ddz.server.api.dto.GameStateResponse;
import com.yourco.ddz.server.core.GameInstance;
import com.yourco.ddz.server.core.GameRegistry;
import com.yourco.ddz.server.ws.dto.*;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
  private static final Logger log = LoggerFactory.getLogger(GameWebSocketHandler.class);

  private final GameRegistry registry;
  private final ObjectMapper objectMapper;

  // Track sessions per game: gameId -> List of sessions
  private final Map<String, List<WebSocketSession>> gameSessions = new ConcurrentHashMap<>();

  // Track player ID per session
  private final Map<String, UUID> sessionPlayerIds = new ConcurrentHashMap<>();

  // Track test mode per game
  private final Map<String, Boolean> gameTestMode = new ConcurrentHashMap<>();

  public GameWebSocketHandler(GameRegistry r, ObjectMapper om) {
    this.registry = r;
    this.objectMapper = om;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    String gameId = extractGameId(session);
    if (gameId == null) {
      session.close(CloseStatus.BAD_DATA);
      return;
    }

    GameInstance game = registry.get(gameId);
    if (game == null) {
      sendError(session, "Game not found: " + gameId);
      session.close(CloseStatus.POLICY_VIOLATION);
      return;
    }

    // Add session to game
    gameSessions.computeIfAbsent(gameId, k -> new ArrayList<>()).add(session);

    // Extract playerId from query params
    UUID playerId = extractPlayerId(session);
    if (playerId != null) {
      sessionPlayerIds.put(session.getId(), playerId);

      // Mark player as connected in game state
      synchronized (game.loop()) {
        game.loop().state().setPlayerConnected(playerId, true);
      }
    }

    // Check for test mode parameter
    boolean testMode = extractTestMode(session);
    if (testMode) {
      gameTestMode.put(gameId, true);
      log.info("Test mode enabled for game {}", gameId);

      // In test mode, mark ALL players as connected (allows single user to control all)
      synchronized (game.loop()) {
        for (UUID pid : game.loop().state().players()) {
          game.loop().state().setPlayerConnected(pid, true);
        }
      }
      log.info("Marked all players as connected for test mode in game {}", gameId);
    }

    log.info(
        "WebSocket connected - gameId: {}, playerId: {}, testMode: {}", gameId, playerId, testMode);

    // Check if all players are now connected and resume game if needed
    if (playerId != null && !testMode) {
      checkAndResumeGame(game, gameId);
    }

    // Send current game state to the newly connected client
    if (playerId != null) {
      GameStateResponse stateResponse =
          GameStateResponse.from(
              game.loop().state(), playerId, game.getMaxBid(), game.maxPlayers());
      sendMessage(session, new GameUpdateMessage(stateResponse, "Connected to game " + gameId));
    } else {
      sendMessage(session, new GameUpdateMessage(null, "Connected to game " + gameId));
    }
  }

  /**
   * Check if all players are connected and resume game if it was paused.
   *
   * @param game The game instance
   * @param gameId The game ID
   */
  private void checkAndResumeGame(GameInstance game, String gameId) {
    synchronized (game.loop()) {
      GameState state = game.loop().state();

      // Check if all players are connected
      boolean allConnected =
          state.players().stream().allMatch(playerId -> state.isPlayerConnected(playerId));

      if (allConnected) {
        // Resume the game
        registry.resumeGame(gameId);
        registry.updateGame(gameId); // Persist the resumed state

        log.info("All players reconnected - resuming game {}", gameId);

        // Broadcast resume to all players
        broadcastStateUpdate(gameId, game, "All players connected - game resumed");
      }
    }
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    String gameId = extractGameId(session);
    UUID playerId = sessionPlayerIds.get(session.getId());

    if (gameId == null) {
      sendError(session, "No game ID in connection");
      return;
    }

    GameInstance game = registry.get(gameId);
    if (game == null) {
      sendError(session, "Game not found: " + gameId);
      return;
    }

    try {
      // Parse incoming message
      GameActionMessage actionMsg =
          objectMapper.readValue(message.getPayload(), GameActionMessage.class);

      // Override playerId from message with session playerId if available
      if (playerId != null) {
        actionMsg.setPlayerId(playerId);
      }

      // Convert to game action and submit
      GameAction action = convertToAction(actionMsg);
      synchronized (game.loop()) {
        game.loop().submit(action);
        game.loop().tick();
      }

      // Persist game state after every action
      registry.updateGame(gameId);

      // Broadcast state update to all players in this game
      broadcastStateUpdate(gameId, game, "Action processed");

    } catch (IllegalStateException | IllegalArgumentException e) {
      log.warn("Invalid action from player {}: {}", playerId, e.getMessage());
      sendError(session, e.getMessage());
    } catch (Exception e) {
      log.error("Error processing WebSocket message", e);
      sendError(session, "Internal server error: " + e.getMessage());
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    String gameId = extractGameId(session);
    UUID playerId = sessionPlayerIds.remove(session.getId());

    if (gameId != null) {
      List<WebSocketSession> sessions = gameSessions.get(gameId);
      if (sessions != null) {
        sessions.remove(session);
        if (sessions.isEmpty()) {
          gameSessions.remove(gameId);
        }
      }

      // In test mode, don't mark players as disconnected (allows player switching)
      boolean isTestMode = Boolean.TRUE.equals(gameTestMode.get(gameId));

      // Mark player as disconnected in game state (unless in test mode)
      if (playerId != null && !isTestMode) {
        GameInstance game = registry.get(gameId);
        if (game != null) {
          synchronized (game.loop()) {
            game.loop().state().setPlayerConnected(playerId, false);
          }

          log.info("Player {} disconnected from game {}", playerId, gameId);

          // Pause the game when any player disconnects
          registry.pauseGame(gameId);
          registry.updateGame(gameId); // Persist the paused state

          // Broadcast disconnect to all remaining players
          broadcastStateUpdate(
              gameId,
              game,
              game.loop().state().getPlayerName(playerId) + " disconnected - game paused");
        }
      } else if (playerId != null && isTestMode) {
        log.info(
            "Player {} closed connection in test mode (not marking as disconnected)", playerId);
      }
    }

    log.info(
        "WebSocket disconnected - gameId: {}, playerId: {}, status: {}", gameId, playerId, status);
  }

  private GameAction convertToAction(GameActionMessage msg) {
    UUID playerId = msg.getPlayerId();
    if (playerId == null) {
      throw new IllegalArgumentException("Player ID is required");
    }

    return switch (msg) {
      case BidMessage bid -> new PlayerAction(playerId, "BID", new Bid(bid.getBidValue()));

      case PlayMessage play -> {
        List<Card> cards = play.getCards().stream().map(dto -> dto.toCard()).toList();
        yield new PlayerAction(playerId, "PLAY", cards);
      }

      case PassMessage pass -> new PlayerAction(playerId, "PLAY", null);

      case SelectLandlordMessage select ->
          new PlayerAction(playerId, "SELECT_LANDLORD", select.getSelectedPlayerId());

      default -> throw new IllegalArgumentException("Unknown action type: " + msg.getClass());
    };
  }

  /**
   * Broadcast game state update to all WebSocket clients connected to this game. This is public so
   * it can be called from REST controllers when game state changes.
   */
  public void broadcastStateUpdate(String gameId, GameInstance game, String message) {
    List<WebSocketSession> sessions = gameSessions.get(gameId);
    if (sessions == null || sessions.isEmpty()) {
      log.warn("No WebSocket sessions found for game {}, cannot broadcast", gameId);
      return;
    }

    GameState state = game.loop().state();
    log.info("Broadcasting state update to {} sessions for game {}", sessions.size(), gameId);

    for (WebSocketSession session : new ArrayList<>(sessions)) {
      if (!session.isOpen()) {
        continue;
      }

      try {
        UUID playerId = sessionPlayerIds.get(session.getId());
        if (playerId == null) {
          // If no player ID, just send minimal state
          sendMessage(session, new GameUpdateMessage(null, message));
        } else {
          // Send personalized state (with player's hand)
          GameStateResponse stateResponse =
              GameStateResponse.from(state, playerId, game.getMaxBid(), game.maxPlayers());
          sendMessage(session, new GameUpdateMessage(stateResponse, message));
        }
      } catch (Exception e) {
        log.error("Error broadcasting to session {}", session.getId(), e);
      }
    }
  }

  private void sendMessage(WebSocketSession session, Object message) {
    try {
      String json = objectMapper.writeValueAsString(message);
      log.info(
          "📤 Sending WebSocket message to session {}: {}",
          session.getId(),
          json.substring(0, Math.min(500, json.length())));
      session.sendMessage(new TextMessage(json));
    } catch (IOException e) {
      log.error("Error sending message to session {}", session.getId(), e);
    }
  }

  private void sendError(WebSocketSession session, String error) {
    sendMessage(session, new ErrorMessage(error));
  }

  private String extractGameId(WebSocketSession session) {
    URI uri = session.getUri();
    if (uri == null) return null;
    String[] parts = uri.getPath().split("/");
    return parts.length > 0 ? parts[parts.length - 1] : null;
  }

  private UUID extractPlayerId(WebSocketSession session) {
    URI uri = session.getUri();
    if (uri == null) return null;

    String query = uri.getQuery();
    if (query == null) return null;

    // Parse query string for playerId parameter
    for (String param : query.split("&")) {
      String[] kv = param.split("=");
      if (kv.length == 2 && "playerId".equals(kv[0])) {
        try {
          return UUID.fromString(kv[1]);
        } catch (IllegalArgumentException e) {
          log.warn("Invalid playerId in query: {}", kv[1]);
        }
      }
    }
    return null;
  }

  private boolean extractTestMode(WebSocketSession session) {
    URI uri = session.getUri();
    if (uri == null) return false;

    String query = uri.getQuery();
    if (query == null) return false;

    // Parse query string for testMode parameter
    for (String param : query.split("&")) {
      String[] kv = param.split("=");
      if (kv.length == 2 && "testMode".equals(kv[0])) {
        return "true".equalsIgnoreCase(kv[1]);
      }
    }
    return false;
  }
}
