package com.yourco.ddz.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yourco.ddz.engine.cards.Card;
import com.yourco.ddz.engine.core.GameState;
import com.yourco.ddz.engine.core.PlayedHand;
import com.yourco.ddz.server.core.GameInstance;
import com.yourco.ddz.server.persistence.Game;
import com.yourco.ddz.server.persistence.GameParticipant;
import com.yourco.ddz.server.persistence.GameResult;
import com.yourco.ddz.server.repository.GameParticipantRepository;
import com.yourco.ddz.server.repository.GameRepository;
import com.yourco.ddz.server.repository.GameResultRepository;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GamePersistenceService {
  private static final Logger log = LoggerFactory.getLogger(GamePersistenceService.class);

  private final GameRepository gameRepository;
  private final GameParticipantRepository participantRepository;
  private final GameResultRepository resultRepository;
  private final ObjectMapper objectMapper;

  public GamePersistenceService(
      GameRepository gameRepository,
      GameParticipantRepository participantRepository,
      GameResultRepository resultRepository,
      ObjectMapper objectMapper) {
    this.gameRepository = gameRepository;
    this.participantRepository = participantRepository;
    this.resultRepository = resultRepository;
    this.objectMapper = objectMapper;
  }

  /**
   * Save or update a game instance to the database. This is called after every game action.
   *
   * @param instance The game instance to persist
   * @param userIdToPlayerIdMap Map of userId to playerId for tracking participants
   */
  @Transactional
  public void saveGame(GameInstance instance, Map<UUID, UUID> userIdToPlayerIdMap) {
    String gameId = instance.gameId();
    GameState state = instance.getState();

    log.debug(
        "Saving game {} - Phase: {}, Players: {}", gameId, state.phase(), state.players().size());

    // Find or create game entity
    Game game = gameRepository.findById(gameId).orElse(new Game());

    if (game.getGameId() == null) {
      game.setGameId(gameId);
      // Generate unique join code (this should come from GameRegistry in reality)
      game.setJoinCode(generateJoinCode(gameId));
      game.setMaxPlayers(instance.maxPlayers());
      log.info("Created new game record for gameId: {}", gameId);
    }

    // Update game state
    game.setCurrentPhase(state.phase().name());
    game.setGameStateJson(serializeGameState(state));

    // Mark as completed if terminated
    if (state.phase() == GameState.Phase.TERMINATED && game.getCompletedAt() == null) {
      game.setCompletedAt(Instant.now());
      log.info("Game {} marked as completed", gameId);

      // Save final scores to game_results table
      saveFinalScores(gameId, state, userIdToPlayerIdMap);
    }

    gameRepository.save(game);
    log.debug("Game {} saved successfully", gameId);
  }

  /**
   * Pause a game (set isPaused flag to true).
   *
   * @param gameId The game ID to pause
   */
  @Transactional
  public void pauseGame(String gameId) {
    Optional<Game> gameOpt = gameRepository.findById(gameId);
    if (gameOpt.isPresent()) {
      Game game = gameOpt.get();
      game.setPaused(true);
      gameRepository.save(game);
      log.info("Game {} paused", gameId);
    } else {
      log.warn("Attempted to pause non-existent game {}", gameId);
    }
  }

  /**
   * Resume a game (set isPaused flag to false).
   *
   * @param gameId The game ID to resume
   */
  @Transactional
  public void resumeGame(String gameId) {
    Optional<Game> gameOpt = gameRepository.findById(gameId);
    if (gameOpt.isPresent()) {
      Game game = gameOpt.get();
      game.setPaused(false);
      gameRepository.save(game);
      log.info("Game {} resumed", gameId);
    } else {
      log.warn("Attempted to resume non-existent game {}", gameId);
    }
  }

  /**
   * Add a participant to a game.
   *
   * @param gameId The game ID
   * @param userId The user ID (persistent user identity)
   * @param playerId The player ID (in-game session identity)
   */
  @Transactional
  public void addParticipant(String gameId, UUID userId, UUID playerId) {
    // Check if participant already exists
    Optional<GameParticipant> existing =
        participantRepository.findByGameIdAndUserId(gameId, userId);

    if (existing.isPresent()) {
      log.debug("Participant already exists: userId={}, gameId={}", userId, gameId);
      return;
    }

    GameParticipant participant = new GameParticipant(gameId, userId, playerId);
    participantRepository.save(participant);
    log.info("Added participant userId={}, playerId={} to game {}", userId, playerId, gameId);
  }

  /**
   * Mark a participant as having left the game.
   *
   * @param gameId The game ID
   * @param userId The user ID
   */
  @Transactional
  public void markParticipantLeft(String gameId, UUID userId) {
    Optional<GameParticipant> participant =
        participantRepository.findByGameIdAndUserId(gameId, userId);

    if (participant.isPresent() && participant.get().isActive()) {
      GameParticipant p = participant.get();
      p.setLeftAt(Instant.now());
      participantRepository.save(p);
      log.info("Marked participant userId={} as left from game {}", userId, gameId);
    }
  }

  /**
   * Get the active game for a user (if any).
   *
   * @param userId The user ID
   * @return Optional containing the game ID if user has an active game
   */
  public Optional<String> getActiveGameForUser(UUID userId) {
    return participantRepository.findActiveGameForUser(userId).map(GameParticipant::getGameId);
  }

  /**
   * Serialize GameState to JSON for storage in the database. Note: This does NOT include scores, as
   * scores are only saved to game_results when the game reaches TERMINATED phase.
   *
   * @param state The game state to serialize
   * @return JsonNode representation of the game state
   */
  private JsonNode serializeGameState(GameState state) {
    ObjectNode json = objectMapper.createObjectNode();

    // Basic game info
    json.put("gameId", state.gameId());
    json.put("phase", state.phase().name());
    json.put("currentPlayerIndex", state.currentPlayerIndex());
    json.put("updatedAt", state.updatedAt().toString());

    // Players (list of UUIDs)
    json.set("players", objectMapper.valueToTree(state.players()));

    // Player metadata
    json.set("playerNames", objectMapper.valueToTree(state.getPlayerNames()));
    json.set("playerConnected", objectMapper.valueToTree(state.getPlayerConnectionStatus()));

    // Hands - serialize each player's cards
    ObjectNode handsNode = objectMapper.createObjectNode();
    for (UUID playerId : state.players()) {
      List<Card> hand = state.handOf(playerId);
      handsNode.set(playerId.toString(), serializeCards(hand));
    }
    json.set("hands", handsNode);

    // Landlord info
    if (state.getLandlordId() != null) {
      json.put("landlordId", state.getLandlordId().toString());
    }
    json.set("landlordIds", objectMapper.valueToTree(state.getLandlordIds()));

    // Bidding state
    if (!state.getAllBids().isEmpty()) {
      ObjectNode bidsNode = objectMapper.createObjectNode();
      state.getAllBids().forEach((playerId, bid) -> bidsNode.put(playerId.toString(), bid));
      json.set("playerBids", bidsNode);
    }

    // Bottom cards
    if (state.bottom() != null && !state.bottom().isEmpty()) {
      json.set("bottom", serializeCards(state.bottom()));
    }

    // Current lead
    if (state.getCurrentLead() != null) {
      json.set("currentLead", serializePlayedHand(state.getCurrentLead()));
    }
    if (state.getCurrentLeadPlayer() != null) {
      json.put("currentLeadPlayer", state.getCurrentLeadPlayer().toString());
    }
    json.put("passesInRow", state.passesInRow());

    // Bomb/rocket counters
    json.put("bombsPlayed", state.getBombsPlayed());
    json.put("rocketsPlayed", state.getRocketsPlayed());
    json.put("landlordPlayed", state.getLandlordPlayed());
    json.put("farmersPlayed", state.getFarmersPlayed());

    // NOTE: Scores are NOT stored here during active play
    // They are only saved to game_results table when phase becomes TERMINATED

    return json;
  }

  /**
   * Serialize a list of cards to JSON array.
   *
   * @param cards List of cards
   * @return JsonNode array of card objects
   */
  private JsonNode serializeCards(List<Card> cards) {
    return objectMapper.valueToTree(
        cards.stream()
            .map(
                card -> {
                  ObjectNode cardNode = objectMapper.createObjectNode();
                  cardNode.put("suit", card.suit().name());
                  cardNode.put("rank", card.rank().name());
                  return cardNode;
                })
            .toList());
  }

  /**
   * Serialize a PlayedHand to JSON.
   *
   * @param hand The played hand
   * @return JsonNode representation of the played hand
   */
  private JsonNode serializePlayedHand(PlayedHand hand) {
    ObjectNode handNode = objectMapper.createObjectNode();
    handNode.put("comboType", hand.type().name());
    handNode.set("cards", serializeCards(hand.cards()));
    return handNode;
  }

  /**
   * Save final scores to the game_results table. Only called when game reaches TERMINATED phase.
   *
   * @param gameId The game ID
   * @param state The final game state
   * @param userIdToPlayerIdMap Map of userId to playerId
   */
  private void saveFinalScores(
      String gameId, GameState state, Map<UUID, UUID> userIdToPlayerIdMap) {
    Map<UUID, Integer> scores = state.getScores();

    if (scores.isEmpty()) {
      log.warn("Game {} completed but no scores available", gameId);
      return;
    }

    // Invert the map to get playerId -> userId
    Map<UUID, UUID> playerIdToUserIdMap = new HashMap<>();
    userIdToPlayerIdMap.forEach((userId, playerId) -> playerIdToUserIdMap.put(playerId, userId));

    for (Map.Entry<UUID, Integer> entry : scores.entrySet()) {
      UUID playerId = entry.getKey();
      int finalScore = entry.getValue();

      UUID userId = playerIdToUserIdMap.get(playerId);
      if (userId == null) {
        log.warn(
            "No userId mapping found for playerId {} in game {}, skipping score save",
            playerId,
            gameId);
        continue;
      }

      // Check if result already exists
      Optional<GameResult> existing =
          resultRepository.findByGameId(gameId).stream()
              .filter(gr -> gr.getUserId().equals(userId))
              .findFirst();

      if (existing.isPresent()) {
        log.debug("Score already saved for userId {} in game {}", userId, gameId);
        continue;
      }

      GameResult result = new GameResult();
      result.setGameId(gameId);
      result.setUserId(userId);
      result.setPlayerId(playerId);
      result.setFinalScore(finalScore);
      result.setWasLandlord(state.isLandlord(playerId));

      resultRepository.save(result);
      log.info(
          "Saved final score for userId {} in game {}: {} (landlord: {})",
          userId,
          gameId,
          finalScore,
          result.isWasLandlord());
    }
  }

  /**
   * Generate a simple join code from gameId. In reality, this should use GameRegistry's join code.
   */
  private String generateJoinCode(String gameId) {
    // This is a placeholder - the real join code should come from GameRegistry
    return gameId.substring(0, Math.min(4, gameId.length())).toUpperCase();
  }
}
