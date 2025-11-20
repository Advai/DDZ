package com.yourco.ddz.server.core;

import com.yourco.ddz.engine.core.DdzRules;
import com.yourco.ddz.engine.core.GameLoop;
import com.yourco.ddz.engine.core.GameState;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record GameInstance(String gameId, GameLoop loop, int maxPlayers) {

  /**
   * Creates a new game instance in LOBBY phase with initial creator.
   *
   * @param gameId unique game identifier
   * @param playerCount number of players for this game
   * @param creatorName name of the game creator
   * @param creatorId UUID of the creator
   * @return new game instance in LOBBY phase
   */
  public static GameInstance create(
      String gameId, int playerCount, String creatorName, UUID creatorId) {
    // Start with just the creator in the lobby
    List<UUID> players = new ArrayList<>();
    players.add(creatorId);

    var state = new GameState(gameId, players);
    state.setPlayerName(creatorId, creatorName);

    // Create rules for the target player count
    var rules = DdzRules.standard(playerCount);
    var loop = new GameLoop(rules, state);

    return new GameInstance(gameId, loop, playerCount);
  }

  public GameState getState() {
    return loop.state();
  }

  public boolean isFull() {
    return getState().players().size() >= maxPlayers;
  }

  public boolean canStart() {
    return getState().players().size() == maxPlayers;
  }
}
