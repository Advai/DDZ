package com.yourco.ddz.server.core;

import com.yourco.ddz.engine.core.DdzRules;
import com.yourco.ddz.engine.core.GameLoop;
import com.yourco.ddz.engine.core.GameState;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameInstance {
  private final String sessionId;
  private String currentGameId;
  private final GameLoop loop;
  private final int maxPlayers;
  private int roundNumber = 1;

  public GameInstance(String sessionId, String gameId, GameLoop loop, int maxPlayers) {
    this.sessionId = sessionId;
    this.currentGameId = gameId;
    this.loop = loop;
    this.maxPlayers = maxPlayers;
  }

  public String sessionId() {
    return sessionId;
  }

  public String gameId() {
    return currentGameId;
  }

  public GameLoop loop() {
    return loop;
  }

  public int maxPlayers() {
    return maxPlayers;
  }

  public String getSessionId() {
    return sessionId;
  }

  public String getCurrentGameId() {
    return currentGameId;
  }

  public int getRoundNumber() {
    return roundNumber;
  }

  public void incrementRound(String newGameId) {
    this.roundNumber++;
    this.currentGameId = newGameId;
  }

  /**
   * Creates a new game instance in LOBBY phase with initial creator.
   *
   * @param sessionId session identifier (constant across rounds)
   * @param gameId unique game identifier for this round
   * @param playerCount number of players for this game
   * @param creatorName name of the game creator
   * @param creatorId UUID of the creator
   * @return new game instance in LOBBY phase
   */
  public static GameInstance create(
      String sessionId, String gameId, int playerCount, String creatorName, UUID creatorId) {
    // Start with just the creator in the lobby
    List<UUID> players = new ArrayList<>();
    players.add(creatorId);

    var state = new GameState(gameId, players);
    state.setPlayerName(creatorId, creatorName);

    // Create rules for the target player count
    var rules = DdzRules.standard(playerCount);
    var loop = new GameLoop(rules, state);

    return new GameInstance(sessionId, gameId, loop, playerCount);
  }

  /**
   * Create an empty game instance with no players.
   *
   * @param sessionId session identifier (constant across rounds)
   * @param gameId The unique game ID for this round
   * @param playerCount Maximum number of players for this game
   * @return A new GameInstance with no players
   */
  public static GameInstance createEmpty(String sessionId, String gameId, int playerCount) {
    // Start with empty player list
    List<UUID> players = new ArrayList<>();

    var state = new GameState(gameId, players);

    // Create rules for the target player count
    var rules = DdzRules.standard(playerCount);
    var loop = new GameLoop(rules, state);

    return new GameInstance(sessionId, gameId, loop, playerCount);
  }

  public GameState getState() {
    return loop.state();
  }

  public DdzRules getRules() {
    return (DdzRules) loop.rules();
  }

  public int getMaxBid() {
    return getRules().getConfig().getMaxBid();
  }

  /**
   * Reconfigure the game rules based on the actual number of players. This should be called before
   * starting the game to ensure the correct ruleset is used.
   *
   * @param actualPlayerCount The actual number of players who joined the game
   */
  public void reconfigureForPlayerCount(int actualPlayerCount) {
    var newRules = DdzRules.standard(actualPlayerCount);
    loop.updateRules(newRules);
  }

  public boolean isFull() {
    return getState().players().size() >= maxPlayers;
  }

  public boolean canStart() {
    return getState().players().size() == maxPlayers;
  }
}
