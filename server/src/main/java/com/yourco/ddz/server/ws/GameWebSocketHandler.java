package com.yourco.ddz.server.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourco.ddz.engine.cards.Card;
import com.yourco.ddz.engine.core.*;
import com.yourco.ddz.server.api.dto.GameStateResponse;
import com.yourco.ddz.server.api.dto.SpectatorGameInfo;
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

  // Track sessions per game session: sessionId -> List of WebSocket sessions
  private final Map<String, List<WebSocketSession>> gameSessions = new ConcurrentHashMap<>();

  // Track player ID per WebSocket session
  private final Map<String, UUID> sessionPlayerIds = new ConcurrentHashMap<>();

  // Track test mode per game session
  private final Map<String, Boolean> gameTestMode = new ConcurrentHashMap<>();

  public GameWebSocketHandler(GameRegistry r, ObjectMapper om) {
    this.registry = r;
    this.objectMapper = om;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    String identifier = extractSessionId(session);
    if (identifier == null) {
      session.close(CloseStatus.BAD_DATA);
      return;
    }

    // Try to get game by sessionId first, then fall back to gameId for backward compat
    GameInstance game = registry.getBySessionId(identifier);
    String sessionId = identifier;

    if (game == null) {
      // Fall back to gameId lookup
      game = registry.get(identifier);
      if (game != null) {
        sessionId = game.getSessionId();
      }
    }

    if (game == null) {
      sendError(session, "Game/Session not found: " + identifier);
      session.close(CloseStatus.POLICY_VIOLATION);
      return;
    }

    // Add session to game (keyed by sessionId)
    gameSessions.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(session);

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
      gameTestMode.put(sessionId, true);
      log.info("Test mode enabled for session {}", sessionId);

      // In test mode, mark ALL players as connected (allows single user to control all)
      synchronized (game.loop()) {
        for (UUID pid : game.loop().state().players()) {
          game.loop().state().setPlayerConnected(pid, true);
        }
      }
      log.info("Marked all players as connected for test mode in session {}", sessionId);
    }

    log.info(
        "WebSocket connected - sessionId: {}, playerId: {}, testMode: {}",
        sessionId,
        playerId,
        testMode);

    // Check if all players are now connected and resume game if needed
    if (playerId != null && !testMode) {
      checkAndResumeGame(game, sessionId);
    }

    // Send current game state to the newly connected client
    String currentGameId = game.getCurrentGameId();
    if (playerId != null) {
      GameStateResponse stateResponse =
          GameStateResponse.from(
              game.loop().state(),
              playerId,
              game.getMaxBid(),
              game.maxPlayers(),
              registry.getSeatPositions(currentGameId));
      sendMessage(
          session, new GameUpdateMessage(stateResponse, "Connected to session " + sessionId));
    } else {
      // Spectator - send basic game info without personal data
      SpectatorGameInfo spectatorInfo = createSpectatorGameInfo(game, currentGameId);
      sendMessage(
          session, new GameUpdateMessage(spectatorInfo, "Connected to session " + sessionId));
    }
  }

  /**
   * Check if all players are connected and resume game if it was paused.
   *
   * @param game The game instance
   * @param sessionId The session ID
   */
  private void checkAndResumeGame(GameInstance game, String sessionId) {
    synchronized (game.loop()) {
      GameState state = game.loop().state();

      // Check if all players are connected
      boolean allConnected =
          state.players().stream().allMatch(playerId -> state.isPlayerConnected(playerId));

      if (allConnected) {
        String currentGameId = game.getCurrentGameId();
        // Resume the game
        registry.resumeGame(currentGameId);
        registry.updateGame(currentGameId); // Persist the resumed state

        log.info(
            "All players reconnected - resuming session {} (game {})", sessionId, currentGameId);

        // Broadcast resume to all players
        broadcastStateUpdate(sessionId, game, "All players connected - game resumed");
      }
    }
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    String identifier = extractSessionId(session);
    UUID playerId = sessionPlayerIds.get(session.getId());

    if (identifier == null) {
      sendError(session, "No session/game ID in connection");
      return;
    }

    // Try sessionId first, fall back to gameId
    GameInstance game = registry.getBySessionId(identifier);
    String sessionId = identifier;
    if (game == null) {
      game = registry.get(identifier);
      if (game != null) {
        sessionId = game.getSessionId();
      }
    }

    if (game == null) {
      sendError(session, "Game/Session not found: " + identifier);
      return;
    }

    String currentGameId = game.getCurrentGameId();

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
      registry.updateGame(currentGameId);

      // Broadcast state update to all players in this session
      broadcastStateUpdate(sessionId, game, "Action processed");

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
    String identifier = extractSessionId(session);
    UUID playerId = sessionPlayerIds.remove(session.getId());

    if (identifier != null) {
      // Try sessionId first, fall back to gameId
      GameInstance game = registry.getBySessionId(identifier);
      String sessionId = identifier;
      if (game == null) {
        game = registry.get(identifier);
        if (game != null) {
          sessionId = game.getSessionId();
        }
      }

      List<WebSocketSession> sessions = gameSessions.get(sessionId);
      if (sessions != null) {
        sessions.remove(session);
        if (sessions.isEmpty()) {
          gameSessions.remove(sessionId);
        }
      }

      // In test mode, don't mark players as disconnected (allows player switching)
      boolean isTestMode = Boolean.TRUE.equals(gameTestMode.get(sessionId));

      // Mark player as disconnected in game state (unless in test mode)
      if (playerId != null && !isTestMode && game != null) {
        String currentGameId = game.getCurrentGameId();
        synchronized (game.loop()) {
          game.loop().state().setPlayerConnected(playerId, false);
        }

        log.info(
            "Player {} disconnected from session {} (game {})", playerId, sessionId, currentGameId);

        // Pause the game when any player disconnects
        registry.pauseGame(currentGameId);
        registry.updateGame(currentGameId); // Persist the paused state

        // Broadcast disconnect to all remaining players
        broadcastStateUpdate(
            sessionId,
            game,
            game.loop().state().getPlayerName(playerId) + " disconnected - game paused");
      } else if (playerId != null && isTestMode) {
        log.info(
            "Player {} closed connection in test mode (not marking as disconnected)", playerId);
      }
    }

    log.info(
        "WebSocket disconnected - sessionId: {}, playerId: {}, status: {}",
        identifier,
        playerId,
        status);
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
   * Broadcast game state update to all WebSocket clients connected to this session. This is public
   * so it can be called from REST controllers when game state changes.
   */
  public void broadcastStateUpdate(String sessionId, GameInstance game, String message) {
    List<WebSocketSession> sessions = gameSessions.get(sessionId);
    if (sessions == null || sessions.isEmpty()) {
      log.warn("No WebSocket sessions found for session {}, cannot broadcast", sessionId);
      return;
    }

    GameState state = game.loop().state();
    String currentGameId = game.getCurrentGameId();
    log.info(
        "Broadcasting state update to {} sessions for session {} (game {})",
        sessions.size(),
        sessionId,
        currentGameId);

    for (WebSocketSession session : new ArrayList<>(sessions)) {
      if (!session.isOpen()) {
        continue;
      }

      try {
        UUID playerId = sessionPlayerIds.get(session.getId());
        if (playerId == null) {
          // Spectator - send basic game info without personal data
          SpectatorGameInfo spectatorInfo = createSpectatorGameInfo(game, currentGameId);
          sendMessage(session, new GameUpdateMessage(spectatorInfo, message));
        } else {
          // Send personalized state (with player's hand)
          GameStateResponse stateResponse =
              GameStateResponse.from(
                  state,
                  playerId,
                  game.getMaxBid(),
                  game.maxPlayers(),
                  registry.getSeatPositions(currentGameId));
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

  private SpectatorGameInfo createSpectatorGameInfo(GameInstance instance, String gameId) {
    GameState state = instance.loop().state();
    var seatPositions = registry.getSeatPositions(gameId);

    return new SpectatorGameInfo(
        gameId,
        state.phase().name(),
        state.players().stream()
            .map(
                p ->
                    new SpectatorGameInfo.BasicPlayerInfo(
                        p.toString(), state.getPlayerName(p), seatPositions.get(p)))
            .toList());
  }

  private String extractGameId(WebSocketSession session) {
    URI uri = session.getUri();
    if (uri == null) return null;
    String[] parts = uri.getPath().split("/");
    return parts.length > 0 ? parts[parts.length - 1] : null;
  }

  private String extractSessionId(WebSocketSession session) {
    // For now, this is the same as extractGameId since the URL path contains the identifier
    // After Phase 6, the URL will be /ws/game/{sessionId} instead of /ws/game/{gameId}
    return extractGameId(session);
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
