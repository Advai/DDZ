package com.yourco.ddz.server.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class GameRegistry {
  private final Map<String, GameInstance> games = new ConcurrentHashMap<>();
  private final Map<String, String> joinCodes = new ConcurrentHashMap<>(); // joinCode -> gameId

  public GameInstance createGame(int playerCount, String creatorName, UUID creatorId) {
    String gameId = "g-" + UUID.randomUUID();
    String joinCode = generateUniqueJoinCode();

    var gameInstance = GameInstance.create(gameId, playerCount, creatorName, creatorId);

    games.put(gameId, gameInstance);
    joinCodes.put(joinCode, gameId);

    return gameInstance;
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

  private String generateUniqueJoinCode() {
    String code;
    do {
      code = JoinCodeGenerator.generate();
    } while (joinCodes.containsKey(code));
    return code;
  }
}
